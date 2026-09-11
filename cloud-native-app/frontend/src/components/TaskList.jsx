// frontend/src/components/TaskList.jsx
import React from 'react';
import TaskItem from './TaskItem';

function TaskList({ tasks, onToggleComplete, onDeleteTask }) {
  if (!tasks || tasks.length === 0) {
    return <p>No tasks yet. Add one!</p>;
  }

  return (
    <ul>
      {tasks.map(task => (
        <TaskItem
          key={task._id}
          task={task}
          onToggleComplete={onToggleComplete}
          onDeleteTask={onDeleteTask}
        />
      ))}
    </ul>
  );
}

export default TaskList;
