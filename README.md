# Batch Jobs Processing Demo

A demonstration project for processing batch operations without message brokers, using database-based job coordination.

## Overview

This project showcases a robust approach to batch job processing that doesn't rely on traditional message brokers like
RabbitMQ or Kafka. Instead, it uses a combination of database locking and either in-memory or Redis-based queues to
coordinate job execution across multiple nodes.

## Key Features

- **No Message Broker Required**: Process batch jobs without the complexity of setting up and maintaining message
  brokers
- **Distributed Job Processing**: Support for multi-node job processing using Redis
- **Transactional Safety**: Jobs are processed within transactions with savepoint support for rollback
- **Concurrency Control**: Configurable concurrency per node
- **Flexible Queue Options**: Choose between in-memory queue (single node) or Redis-based queue (distributed)
- **Job Status Reporting**: Built-in job status reporting
- **Error Handling**: Robust error handling with automatic job failure tracking

## How It Works

### Architecture

The system consists of several key components:

1. **Job Entity**: Represents a job with a unique ID, type, target data, and status
2. **Job Persistence Service**: Handles job creation and status updates
3. **Job Queue**: Manages the queue of job IDs to be processed (in-memory or Redis-based)
4. **Job Execution Service**: Manages worker threads that process jobs from the queue
5. **Job Processors**: Type-specific implementations that handle the actual job processing logic

### Job Processing Flow

1. A job is created and persisted to the database
2. The job ID is added to the queue
3. Worker threads poll the queue for job IDs
4. When a job ID is retrieved, the worker:
    - Locks the job in the database using `FOR UPDATE SKIP LOCKED`
    - Processes the job using the appropriate job processor
    - Updates the job status (succeeded or failed)

### Distributed Processing

When configured to use Redis:

- The job queue is backed by a Redis distributed queue
- Multiple nodes can process jobs concurrently
- Database locking ensures each job is processed only once

## Demo Use Cases

This project demonstrates how to implement:

- Batch processing of tasks
- Distributed work coordination
- Resilient job processing with error handling
- Scalable processing across multiple nodes

## Benefits Over Message Brokers

- **Simplicity**: No need to set up and maintain a separate message broker
- **Transactional Safety**: Jobs and their status are managed within database transactions
- **Reduced Infrastructure**: Fewer components to manage and monitor
- **Consistency**: Job state is always consistent with the database state
