-- Demo data. Dates are relative to load time so events stay "upcoming".

INSERT INTO app_user (name, email) VALUES
    ('Alice Tan',  'alice@example.com'),
    ('Bob Lim',    'bob@example.com'),
    ('Ah Girl', 'girl@example.com'),
    ('Ah Boy', 'boy@example.com');

INSERT INTO event (name, venue, event_date, total_seats, available_seats, version) VALUES
    ('Esplanade Symphony Night',  'Esplanade Concert Hall', NOW() + INTERVAL '14 days', 200, 200, 0),
    ('Golden Village Film Gala',  'GV VivoCity',            NOW() + INTERVAL '7 days',  120, 45,  0),
    ('Shaw Classics Retrospective','Shaw Lido',             NOW() + INTERVAL '21 days', 80,  80,  0),
    ('Arts Theatre Musical',       'Arts Theatre',          NOW() + INTERVAL '4 days',  150, 4,   0),
    -- Fully booked: should NOT appear in the bookable list (US1 filter).
    ('Sold-Out Jazz Evening',      'Arts Theatre',          NOW() + INTERVAL '10 days', 60,  0,   0),
    -- Past event: should NOT appear in the bookable list (US1 filter).
    ('Last Month Drama',           'Drama Centre',          NOW() - INTERVAL '5 days',  100, 30,  0);
