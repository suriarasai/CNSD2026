import React from 'react';

const BookingList = ({ bookings, onDelete }) => {
  if (bookings.length === 0) {
    return <div style={styles.empty}>No bookings yet. Create your first booking!</div>;
  }

  return (
    <div style={styles.list}>
      <h2 style={styles.listTitle}>Your Bookings</h2>
      <div style={styles.grid}>
        {bookings.map(booking => (
          <div key={booking._id} style={styles.card}>
            <div style={styles.cardHeader}>
              <h3 style={styles.destination}>{booking.destination}</h3>
              <button
                onClick={() => onDelete(booking._id)}
                style={styles.deleteButton}
              >
                ×
              </button>
            </div>
            <div style={styles.cardBody}>
              <p><strong>Traveler:</strong> {booking.traveler_name}</p>
              <p><strong>Dates:</strong> {booking.start_date} to {booking.end_date}</p>
              <p><strong>Travelers:</strong> {booking.num_travelers}</p>
              <p style={styles.status}>Status: {booking.status}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

const styles = {
  list: {
    marginTop: '2rem',
  },
  listTitle: {
    color: 'white',
    marginBottom: '1.5rem',
  },
  empty: {
    background: 'white',
    padding: '3rem',
    borderRadius: '12px',
    textAlign: 'center',
    color: '#666',
    fontSize: '1.1rem',
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))',
    gap: '1.5rem',
  },
  card: {
    background: 'white',
    borderRadius: '12px',
    padding: '1.5rem',
    boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
    transition: 'transform 0.3s',
  },
  cardHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'start',
    marginBottom: '1rem',
  },
  destination: {
    color: '#667eea',
    fontSize: '1.5rem',
    margin: 0,
  },
  deleteButton: {
    background: '#ff5252',
    color: 'white',
    border: 'none',
    borderRadius: '50%',
    width: '30px',
    height: '30px',
    fontSize: '1.5rem',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    lineHeight: 1,
  },
  cardBody: {
    color: '#555',
    lineHeight: 1.8,
  },
  status: {
    marginTop: '1rem',
    padding: '0.5rem',
    background: '#e8f5e9',
    borderRadius: '4px',
    color: '#2e7d32',
  },
};

export default BookingList;
