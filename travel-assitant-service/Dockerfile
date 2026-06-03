# Dockerfile

# 1. Use an official Node.js runtime as a parent image
# Using a specific LTS version is good practice (e.g., 20-alpine, 22-alpine)
# Alpine versions are smaller
FROM node:20-alpine

# 2. Set the working directory in the container
WORKDIR /usr/src/app

# 3. Copy package.json and package-lock.json (or yarn.lock)
# This step is separated to leverage Docker's build cache
# If these files don't change, Docker won't re-run npm install
COPY package*.json ./

# 4. Install application dependencies
# If you were using yarn, it would be: RUN yarn install --frozen-lockfile
RUN npm install
# For production, you might consider: RUN npm ci --only=production
# 'npm ci' is generally faster and more reliable for CI/CD

# 5. Bundle app source inside the Docker image
COPY . .

# 6. Expose the port the app runs on
# This should match the port your Express app listens on
EXPOSE 3000

# 7. Define the command to run your app when the container starts
# This will execute `node index.js`
CMD [ "node", "index.js" ]