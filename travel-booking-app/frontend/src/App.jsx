import React, { useState, useEffect } from 'react';
import Header from './components/Header';
import BookingForm from './components/BookingForm';
import BookingList from './components/BookingList';
import { getBookings, createBooking, deleteBooking } from './services/api';

function App() {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadBookings();
  }, []);

  const loadBookings = async () => {
    try {
      setLoading(true);
      const data = await getBookings();
      setBookings(data);
      setError(null);
    } catch (err) {
      setError('Failed to load bookings');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateBooking = async (bookingData) => {
    try {
      await createBooking(bookingData);
      await loadBookings();
    } catch (err) {
      setError('Failed to create booking');
      console.error(err);
    }
  };

  const handleDeleteBooking = async (id) => {
    try {
      await deleteBooking(id);
      await loadBookings();
    } catch (err) {
      setError('Failed to delete booking');
      console.error(err);
    }
  };

  return (
    <div className="app">
      <Header />
      <main className="container">
        <BookingForm onSubmit={handleCreateBooking} />
        {error && <div className="error">{error}</div>}
        {loading ? (
          <div className="loading">Loading bookings...</div>
        ) : (
          <BookingList bookings={bookings} onDelete={handleDeleteBooking} />
        )}
      </main>
    </div>
  );
}

export default App;