import React from 'react';

const Header = () => {
  return (
    <header style={styles.header}>
      <div style={styles.container}>
        <h1 style={styles.title}>✈️ Travel Booking</h1>
        <p style={styles.subtitle}>Book your next adventure</p>
      </div>
    </header>
  );
};

const styles = {
  header: {
    background: 'rgba(255, 255, 255, 0.1)',
    backdropFilter: 'blur(10px)',
    padding: '2rem 0',
    borderBottom: '1px solid rgba(255, 255, 255, 0.2)',
  },
  container: {
    maxWidth: '1200px',
    margin: '0 auto',
    padding: '0 2rem',
  },
  title: {
    color: 'white',
    fontSize: '2.5rem',
    marginBottom: '0.5rem',
  },
  subtitle: {
    color: 'rgba(255, 255, 255, 0.8)',
    fontSize: '1.1rem',
  },
};

export default Header;
