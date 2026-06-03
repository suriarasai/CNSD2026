// frontend/src/components/TaskItem.jsx
import React from 'react';

function TaskItem({ task, onToggleComplete, onDeleteTask }) {
  return (
    <li style={{ textDecoration: task.completed ? 'line-through' : 'none' }}>
      {task.title}
      <button onClick={() => onToggleComplete(task)}>
        {task.completed ? 'Undo' : 'Complete'}
      </button>
      <button onClick={() => onDeleteTask(task._id)}>
        Delete
      </button>
    </li>
  );
}

export default TaskItem;
