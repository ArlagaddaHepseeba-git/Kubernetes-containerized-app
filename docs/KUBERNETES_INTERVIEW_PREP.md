# Kubernetes Interview Preparation

A project on a resume is only worth what you can explain. These are the
questions an interviewer will actually ask about this project, with the
answers you need to be able to give out loud.

Practise out loud, not in your head. If you cannot say the answer in two or
three sentences without reading, you do not know it yet.

---

## 1. Architecture

**Q: What is the control plane and what is a worker node?**

The control plane is the brain. It runs the API server, which is the only
component clients talk to, plus etcd which stores all cluster state, the
scheduler which decides which node each pod runs on, and the controller
manager which reconciles desired state with actual state.

A worker node is a machine that actually runs your containers. It runs the
kubelet, which talks to the API server and starts and stops containers, and a
container runtime such as containerd.

Everything goes through the API server. When you run `kubectl apply`, your
request hits the API server, which writes to etcd, and controllers notice the
change and act on it. There is no direct kubelet to scheduler communication.

**Q: What actually happens when you `kubectl apply` a Deployment with 3 replicas?**

1. The API server stores the Deployment in etcd
2. The Deployment controller creates a ReplicaSet
3. The ReplicaSet controller creates 3 Pods
4. The scheduler assigns each pod to a node
5. The kubelet on that node pulls the image and starts the container
6. The readiness probe passes, and kube-proxy adds the pod to the Service
   endpoints

Each of those is a separate controller acting independently. That is the
reconcile loop: keep declaring desired state and let controllers converge.

**Q: Pod vs Deployment?**

A Pod is the smallest thing Kubernetes can schedule. It is one or more
containers that always run together on one node. If the node dies, the pod
dies with it, and Kubernetes replaces it only if a controller owns it.

A Deployment manages a ReplicaSet, which keeps a specified number of identical
pods running and handles rolling updates. You almost never create a bare Pod
in production, because nothing would replace it if it died.

**Q: When would you use a StatefulSet instead?**

When a workload needs stable identity or stable storage. MySQL is the example
here: a pod needs its data to survive rescheduling, which means a
PersistentVolumeClaim bound to that specific pod. StatefulSets also give
predictable hostnames, like `mysql-0`, `mysql-1`.

The opposite: a Deployment is right when pods are interchangeable. Two
backend replicas serving the same requests are fungible, so a Deployment is
correct and simpler.

---

## 2. Probes — the most common follow-up

**Q: What is the difference between liveness and readiness?**

Liveness answers "is this process broken?" If it fails, Kubernetes kills and
restarts the container.

Readiness answers "should this pod get traffic right now?" If it fails,
Kubernetes removes the pod from the Service endpoints but does not restart
it.

A pod can be alive but not ready. A pod that just lost database connectivity
is running fine and should keep running, but it should stop receiving
requests, or every request routed to it fails.

**Q: Why did you add a startup probe?**

Because Spring Boot takes 30 to 60 seconds to boot. Without a startup probe,
liveness starts checking at an early `initialDelaySeconds` and the app has not
answered yet, so Kubernetes kills it. It restarts, boots slowly, gets killed
again, and the pod never comes up. This is a real failure mode I hit.

A startup probe holds liveness off until it passes. Only then does liveness
begin. It is effectively a third, more forgiving question: "has it started
yet?"

**Q: What happens if all readiness probes fail?**

The pod stays running but is removed from every Service that selects it, so no
traffic reaches it. The Deployment is still happy, because readiness failures
do not trigger a restart. If the underlying problem is fixed, the probe
passes again and the pod rejoins the Service automatically.

---

## 3. Services and networking

**Q: What does a Service actually do?**

It gives a stable virtual IP and DNS name to a changing set of pods. Pod IPs
change every time a pod is recreated, so nothing could hardcode them.

kube-proxy watches for pods matching a Service selector and programs
iptables or IPVS rules on every node to forward Service traffic to the ready
pod IPs. The Service IP never changes even as the pods behind it do.

**Q: What is the difference between ClusterIP, NodePort, and LoadBalancer?**

- **ClusterIP** is the default, cluster-internal only. Used by the backend
  Service here.
- **NodePort** opens the Service on a port on every node, so you can reach it
  with a node IP. Used by the frontend here because it needs to be reachable
  without installing an Ingress controller.
- **LoadBalancer** provisions a cloud load balancer in front of the Service.
  That is what a cloud provider does when it sees this type.

An **Ingress** is different from all three. It routes HTTP by path or hostname
above the Service layer, so one IP can front many Services. That is why
`/api` goes to the backend and `/` goes to the frontend in the AWS Ingress.

**Q: Why did you avoid a headless Service for MySQL?**

A headless Service, `clusterIP: None`, does not provide a virtual IP. DNS
returns the pod IPs directly. That is useful for StatefulSets where you want
to address pod 0 individually, but wrong for a client connection, because the
pod IP changes when the pod is recreated and the client would be holding a
stale address.

For the backend connecting to MySQL, a normal ClusterIP is correct. It is a
stable address that load-balances across the database pods.

---

## 4. Storage and state

**Q: Why does MySQL need a StatefulSet?**

Because it needs a PersistentVolumeClaim. A PVC is a request for storage that
is bound to a specific pod and follows it wherever it is scheduled. A
Deployment with an `emptyDir` loses all data every time the pod restarts, which
is unacceptable for a database.

**Q: What is a PVC, and what are the access modes?**

A PersistentVolumeClaim is a request for storage. It is how a pod asks for a
volume without knowing anything about the underlying infrastructure. That
separation is deliberate: the pod says "I need 5 GB, ReadWriteOnce" and the
platform decides whether that is an EBS volume, an EFS mount, or something
else.

Access modes: `ReadWriteOnce` means one node can mount it read-write, which
fits a single database pod. `ReadWriteMany` allows many nodes, which you would
need for something like a shared file system. `ReadOnlyMany` is for
read-only mounts.

---

## 5. Scaling

**Q: What does the HPA do here?**

It watches the backend pods and keeps CPU utilisation at 70%, between 2 and 6
replicas. Above 70% it adds a pod, below it removes one.

**Q: What is a limitation of the HPA?**

It needs metrics-server running in the cluster, because that is where the
resource usage numbers come from. The HPA does not collect metrics itself.

**Q: What is a caveat about CPU-based HPA on a Java app?**

The request is `averageUtilization` across pods, meaning utilisation against
the *request*, not the limit. If you set a request of 250m and utilisation
hovers near the request, the HPA scales on a very small absolute number and
becomes twitchy. Requests should be set to something realistic, and JVM heap
sizing interacts with this too, since a JVM that is not memory-bound may
appear to have plenty of headroom.

---

## 6. Helm

**Q: Why Helm instead of plain manifests?**

Helm packages a set of related manifests as a single versioned release, with
values externalised and upgrade and rollback built in. Without it, changing
the image tag means editing YAML by hand on every environment, and there is no
release history to roll back to.

**Q: What is the difference between `helm install` and `helm upgrade`?**

`install` creates a new release and fails if one already exists with that
name. `upgrade` updates an existing release, and creates it if it does not
exist. `helm upgrade --install` does both, which is why the deploy script uses
it: the same command works for a first deploy and a later update.

**Q: How does rollback work?**

Every install and upgrade is stored as a numbered revision with the rendered
manifests and the values used. `helm rollback threetier 1` re-applies
revision 1. It is a fast redeploy of known-good state, not a diff.

**Q: What is the template function pattern in the chart?**

`_helpers.tpl` defines named templates using `define`, such as the app name and
the standard label set. Templates then call `include` to reuse them. The
alternative is repeating the same label block in every file, which drifts out
of sync over time.

**Q: Why is `DB_URL` built in the template rather than in values.yaml?**

Because it contains the release name. The Service the StatefulSet creates is
named `<release-name>-mysql`, so hardcoding a host in `values.yaml` breaks the
moment someone installs the chart under a different name. Building it in the
template means it always matches.

---

## 7. Rolling updates

**Q: What does `maxSurge: 1, maxUnavailable: 0` do?**

It allows one extra pod to exist during the update, and never reduces the
number of serving pods. Kubernetes brings up the new pod, waits for it to pass
readiness, and only then removes an old one. A request in flight during the
switch is never dropped.

The default, `maxUnavailable: 1`, would delete a pod before the replacement
is ready, which means brief downtime and possible errors.

**Q: How would you roll back a bad deployment?**

`kubectl rollout undo deployment/backend`. Kubernetes keeps the previous
ReplicaSet's ReplicaSet revision history, so this re-applies the last good
state. With Helm it is `helm rollback`.

You can also check progress with `kubectl rollout status` and
`kubectl rollout history`.

---

## 8. Security

**Q: Where are the database credentials?**

They come from a Kubernetes Secret, referenced by the Deployment through
`envFrom: secretRef`. The manifest holds a placeholder value. In a real
environment the External Secrets Operator syncs the Secret from AWS Secrets
Manager, so nothing sensitive is stored in git.

**Q: Why `runAsNonRoot`?**

A container running as root that gets compromised has more power over the node
than it needs. Running as an unprivileged user limits the blast radius. The
backend Dockerfile already creates a non-root user for the same reason.

**Q: What would you add for a production cluster?**

NetworkPolicies to restrict which pods can talk to which, RBAC so service
accounts only have the permissions they need, Pod Security Admission, image
signing with Cosign verified at admission, and disabling automatic token
mounting where the container does not need the API.

---

## 9. Troubleshooting — expect these

**Q: A pod is in `ImagePullBackOff`. What is happening?**

The node cannot pull the image. Either the tag does not exist, or the image
lives only in a local Docker daemon and not in the cluster runtime. On a local
cluster, a locally built image must be explicitly loaded, with
`minikube image load` for minikube or `k3s ctr images import` for k3s. On EKS
the image must be in ECR and referenced by full tag.

**Q: A pod is in `CrashLoopBackOff`.**

The container starts and immediately exits. Usually a crash, a bad config, or a
failed dependency. `kubectl logs <pod> --previous` shows the output of the
previous attempt, which is usually the useful one.

**Q: Pods are `Pending`.**

Usually no node has enough capacity for the resource request. Check
`kubectl describe pod` for a scheduling event. Also check PVC binding, since a
Pending pod with an unbound PVC looks the same.

**Q: A pod is `Running` but the app returns 503.**

It is running but failing readiness, so it is not in the Service endpoints.
Check `kubectl describe pod` for the probe error. A common cause is the
database not being ready, which is exactly why the readiness group includes
the database check.

**Q: `helm install` fails with "another release already exists".**

The release name is taken. Either `helm upgrade --install` to update it, or
`helm uninstall` first if you want a clean install.

---

## 10. The one you must be honest about

If you have not actually run this, say so.

> "I wrote the manifests and the chart, and I have not deployed it to a real
> cluster yet. The design decisions I would expect to matter are the startup
> probe for the slow JVM boot, and the ClusterIP on the database Service
> rather than a headless one."

That answer is worth more than claiming experience you do not have. It shows
you understand the design, and it is honest. An interviewer who catches you
guessing ends the conversation. An interviewer who catches you being straight
about your gaps will often keep talking.

Run it once and this whole page becomes unnecessary.
