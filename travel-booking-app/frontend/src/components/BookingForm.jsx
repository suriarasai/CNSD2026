import React, { useState } from 'react';

const BookingForm = ({ onSubmit }) => {
  const [formData, setFormData] = useState({
    destination: '',
    traveler_name: '',
    start_date: '',
    end_date: '',
    num_travelers: 1,
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit(formData);
    setFormData({
      destination: '',
      traveler_name: '',
      start_date: '',
      end_date: '',
      num_travelers: 1,
    });
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: name === 'num_travelers' ? parseInt(value) : value
    }));
  };

  return (
    <form onSubmit={handleSubmit} style={styles.form}>
      <h2 style={styles.formTitle}>New Booking</h2>
      <div style={styles.formGrid}>
        <input
          type="text"
          name="destination"
          placeholder="Destination"
          value={formData.destination}
          onChange={handleChange}
          required
          style={styles.input}
        />
        <input
          type="text"
          name="traveler_name"
          placeholder="Traveler Name"
          value={formData.traveler_name}
          onChange={handleChange}
          required
          style={styles.input}
        />
        <input
          type="date"
          name="start_date"
          value={formData.start_date}
          onChange={handleChange}
          required
          style={styles.input}
        />
        <input
          type="date"
          name="end_date"
          value={formData.end_date}
          onChange={handleChange}
          required
          style={styles.input}
        />
        <input
          type="number"
          name="num_travelers"
          min="1"
          value={formData.num_travelers}
          onChange={handleChange}
          required
          style={styles.input}
        />
      </div>
      <button type="submit" style={styles.button}>Create Booking</button>
    </form>
  );
};

const styles = {
  form: {
    background: 'white',
    padding: '2rem',
    borderRadius: '12px',
    marginBottom: '2rem',
    boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
  },
  formTitle: {
    marginBottom: '1.5rem',
    color: '#333',
  },
  formGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
    gap: '1rem',
    marginBottom: '1.5rem',
  },
  input: {
    padding: '0.75rem',
    border: '2px solid #e0e0e0',
    borderRadius: '8px',
    fontSize: '1rem',
    transition: 'border-color 0.3s',
  },
  button: {
    width: '100%',
    padding: '1rem',
    background: '#667eea',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    fontSize: '1.1rem',
    cursor: 'pointer',
    transition: 'background 0.3s',
  },
};

export default BookingForm;