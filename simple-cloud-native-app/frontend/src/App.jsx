// frontend/src/App.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';
// import TaskList from './components/TaskList'; // You would create these
// import AddTaskForm from './components/AddTaskForm';

// Define the backend API URL. When running with compose,
// the browser still accesses it via localhost and the mapped port.
// If your frontend Nginx is on 3000 and backend on 3001 (mapped from container):
const API_URL = 'http://localhost:3001/tasks';

function App() {
  const [tasks, setTasks] = useState([]);
  const [newTaskTitle, setNewTaskTitle] = useState('');

  useEffect(() => {
    fetchTasks();
  }, []);

  const fetchTasks = async () => {
    try {
      const response = await axios.get(API_URL);
      setTasks(response.data);
    } catch (error) {
      console.error("Error fetching tasks:", error);
    }
  };

  const handleAddTask = async (e) => {
    e.preventDefault();
    if (!newTaskTitle.trim()) return;
    try {
      const response = await axios.post(API_URL, { title: newTaskTitle });
      setTasks([...tasks, response.data]);
      setNewTaskTitle('');
    } catch (error) {
      console.error("Error adding task:", error);
    }
  };

  const toggleComplete = async (task) => {
    try {
      const updatedTask = { ...task, completed: !task.completed };
      const response = await axios.put(`${API_URL}/${task._id}`, updatedTask);
      setTasks(tasks.map(t => t._id === task._id ? response.data : t));
    } catch (error) {
      console.error("Error updating task:", error);
    }
  };

   const handleDeleteTask = async (taskId) => {
    try {
      await axios.delete(`${API_URL}/${taskId}`);
      setTasks(tasks.filter(task => task._id !== taskId));
    } catch (error) {
      console.error("Error deleting task:", error);
    }
  };

  return (
    <div className="App">
      <h1>Task Manager</h1>
      <form onSubmit={handleAddTask}>
        <input
          type="text"
          value={newTaskTitle}
          onChange={(e) => setNewTaskTitle(e.target.value)}
          placeholder="Add a new task"
        />
        <button type="submit">Add Task</button>
      </form>
      <ul>
        {tasks.map(task => (
          <li key={task._id} style={{ textDecoration: task.completed ? 'line-through' : 'none' }}>
            {task.title}
            <button onClick={() => toggleComplete(task)}>
              {task.completed ? 'Undo' : 'Complete'}
            </button>
            <button onClick={() => handleDeleteTask(task._id)}>Delete</button>
          </li>
        ))}
      </ul>
    </div>
  );
}

export default App;