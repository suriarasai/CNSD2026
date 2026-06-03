// backend/server.js
require('dotenv').config();

const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const logger = require('./logger'); // <-- Import the logger

const app = express();
const port = process.env.PORT || 3001;

app.use(cors());
app.use(express.json());

// MongoDB Connection
mongoose.connect(process.env.MONGO_URI)
  .then(() => logger.info('MongoDB connected successfully')) // <-- Use logger
  .catch(err => logger.error('MongoDB connection error:', { error: err.message })); // <-- Use logger

// Define Mongoose Schema and Model for Tasks
const taskSchema = new mongoose.Schema({
  title: { type: String, required: true },
  completed: { type: Boolean, default: false }
});
const Task = mongoose.model('Task', taskSchema);

// --- API Routes ---
app.post('/tasks', async (req, res) => {
  try {
    const task = new Task(req.body);
    await task.save();
    logger.info('Task created successfully', { taskId: task._id }); // <-- Use logger
    res.status(201).send(task);
  } catch (error) {
    logger.error('Error creating task', { error: error.message, body: req.body }); // <-- Use logger
    res.status(400).send({ message: 'Error creating task', error: error.message });
  }
});

app.get('/tasks', async (req, res) => {
  try {
    const tasks = await Task.find();
    res.send(tasks);
  } catch (error) {
    logger.error('Error fetching tasks', { error: error.message }); // <-- Use logger
    res.status(500).send({ message: 'Error fetching tasks', error: error.message });
  }
});

app.put('/tasks/:id', async (req, res) => {
  try {
    const task = await Task.findByIdAndUpdate(req.params.id, req.body, { new: true, runValidators: true });
    if (!task) {
      logger.warn('Update failed: Task not found', { taskId: req.params.id }); // <-- Use logger
      return res.status(404).send({ message: 'Task not found' });
    }
    logger.info('Task updated successfully', { taskId: task._id }); // <-- Use logger
    res.send(task);
  } catch (error) {
    logger.error('Error updating task', { error: error.message, taskId: req.params.id }); // <-- Use logger
    res.status(400).send({ message: 'Error updating task', error: error.message });
  }
});

app.delete('/tasks/:id', async (req, res) => {
  try {
    const task = await Task.findByIdAndDelete(req.params.id);
    if (!task) {
      logger.warn('Delete failed: Task not found', { taskId: req.params.id }); // <-- Use logger
      return res.status(404).send({ message: 'Task not found' });
    }
    logger.info('Task deleted successfully', { taskId: req.params.id }); // <-- Use logger
    res.send({ message: 'Task deleted successfully' });
  } catch (error) {
    logger.error('Error deleting task', { error: error.message, taskId: req.params.id }); // <-- Use logger
    res.status(500).send({ message: 'Error deleting task', error: error.message });
  }
});

app.listen(port, () => {
  logger.info(`Backend server running on http://localhost:${port}`); // <-- Use logger
});