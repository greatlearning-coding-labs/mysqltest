-- Create table
CREATE TABLE users (
    id INT PRIMARY KEY,
    name VARCHAR(100)
);

-- Insert sample data
INSERT INTO users (id, name) VALUES (1, 'Alice'), (2, 'Bob');

-- Select queries
SELECT COUNT(*) FROM users;
SELECT name FROM users WHERE id = 1;
