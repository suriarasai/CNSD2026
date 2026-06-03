from datetime import datetime
from bson import ObjectId

class Booking:
    def __init__(self, destination, traveler_name, start_date, end_date, num_travelers=1):
        self.destination = destination
        self.traveler_name = traveler_name
        self.start_date = start_date
        self.end_date = end_date
        self.num_travelers = num_travelers
        self.status = 'confirmed'
        self.created_at = datetime.utcnow()
        self.updated_at = datetime.utcnow()
    
    def to_dict(self):
        return {
            'destination': self.destination,
            'traveler_name': self.traveler_name,
            'start_date': self.start_date,
            'end_date': self.end_date,
            'num_travelers': self.num_travelers,
            'status': self.status,
            'created_at': self.created_at,
            'updated_at': self.updated_at
        }
    
    @staticmethod
    def from_dict(data):
        booking = Booking(
            data['destination'],
            data['traveler_name'],
            data['start_date'],
            data['end_date'],
            data.get('num_travelers', 1)
        )
        if 'status' in data:
            booking.status = data['status']
        return booking