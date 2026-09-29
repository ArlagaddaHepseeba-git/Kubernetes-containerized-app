import React, { useState, useEffect } from 'react';
import axios from 'axios';

const API_URL = '/api';
function App() {
  const [items, setItems] = useState([]);
  const [name, setName] = useState('');
  const [status, setStatus] = useState('Checking...');
  const [error, setError] = useState('');

  useEffect(() => {
    fetchItems();
    checkHealth();
  }, []);

  const checkHealth = async () => {
    try {
      const res = await axios.get(`${API_URL}/health`);
      setStatus(res.data.status);
    } catch (err) {
      setStatus('Unreachable');
    }
  };

  const fetchItems = async () => {
    try {
      const res = await axios.get(`${API_URL}/items`);
      setItems(res.data);
    } catch (err) {
      setError('Could not load items. Is the backend running?');
      console.error(err);
    }
  };

  const addItem = async (e) => {
    e.preventDefault();
    if (!name.trim()) return;
    try {
      const res = await axios.post(`${API_URL}/items`, { name });
      setItems([...items, res.data]);
      setName('');
    } catch (err) {
      setError('Could not add item.');
      console.error(err);
    }
  };

  const deleteItem = async (id) => {
    try {
      await axios.delete(`${API_URL}/items/${id}`);
      setItems(items.filter((item) => item.id !== id));
    } catch (err) {
      setError('Could not delete item.');
      console.error(err);
    }
  };

  return (
    <div style={{ maxWidth: 600, margin: '40px auto', padding: '0 16px' }}>
      <h1>DevOps Practice App</h1>
      <p>
        Backend status:{' '}
        <strong style={{ color: status === 'healthy' ? 'green' : 'red' }}>{status}</strong>
      </p>

      <form onSubmit={addItem} style={{ marginBottom: 24 }}>
        <input
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Enter item name..."
          style={{ padding: 8, width: 280, marginRight: 8, borderRadius: 4, border: '1px solid #ccc' }}
        />
        <button
          type="submit"
          style={{ padding: '8px 16px', background: '#1a73e8', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer' }}
        >
          Add
        </button>
      </form>

      {error && <p style={{ color: 'red' }}>{error}</p>}

      <ul style={{ listStyle: 'none', padding: 0 }}>
        {items.map((item) => (
          <li
            key={item.id}
            style={{
              padding: 12,
              border: '1px solid #ddd',
              borderRadius: 4,
              marginBottom: 8,
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              background: '#fff',
            }}
          >
            <span>{item.name}</span>
            <button
              onClick={() => deleteItem(item.id)}
              style={{ color: '#d32f2f', border: 'none', background: 'none', cursor: 'pointer' }}
            >
              Delete
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}

export default App;
