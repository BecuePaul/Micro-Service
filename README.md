# Micro-Commerce Application

This project is a microservice-based e-commerce application built with Java and Spring Boot. It includes an API Gateway, a RabbitMQ message broker, and a simple frontend for testing.

## Architecture

The application is composed of several services:

- **API Gateway (`gateway-service`)**: The single entry point for all client requests, running on port `8080`. It routes traffic to the appropriate microservice.
- **Product Service (`product-service`)**: Manages the product catalog.
- **Customer Service (`customer-service`)**: Manages customer information.
- **Order Service (`order-service`)**: Manages customer orders and communicates with other services for data validation.
- **RabbitMQ**: A message broker for asynchronous communication (e.g., order notifications).
- **Frontend**: A simple web interface for testing, available on port `8088`.

Each microservice is autonomous and has its own PostgreSQL database.

## Prerequisites

- Java 17
- Maven
- Docker
- Docker Compose

## Running the Application

The easiest way to run the application is by using Docker Compose. This will build and run all services in a containerized environment.

From the root directory of the project, run:
```bash
docker compose up --build -d
```
This command builds the images and starts all services in detached mode.

### Access Points
- **API Gateway**: `http://localhost:8080`
- **Test Frontend**: `http://localhost:8088`
- **RabbitMQ Management**: `http://localhost:15672` (user: `guest`, pass: `guest`)

## How to Use

### 1. Using the Test Frontend

The simplest way to interact with the application is through the web interface. Open your browser and navigate to **[http://localhost:8088](http://localhost:8088)**. You can create customers, products, and place orders directly from this page.

### 2. Using `curl`

You can also use `curl` to interact with the API Gateway. The APIs are secured with HTTP Basic Authentication. The credentials are `admin:password`.

**Note**: All requests should be sent to the API Gateway on port `8080`.

#### Create a Product
```bash
curl -u admin:password -X POST http://localhost:8080/products -H "Content-Type: application/json" -d '{"name":"Laptop","description":"A powerful laptop","price":1200.50, "stock": 50}'
```

#### Create a Customer
```bash
curl -u admin:password -X POST http://localhost:8080/customers -H "Content-Type: application/json" -d '{"firstName":"John","lastName":"Doe","email":"john.doe@example.com"}'
```

#### Create an Order
Make sure to use the `id` of an existing customer and product.
```bash
curl -u admin:password -X POST http://localhost:8080/orders -H "Content-Type: application/json" -d '{"customerId":1,"orderItems":[{"productId":1,"quantity":2}]}'
```

#### Get All Products
```bash
curl -u admin:password http://localhost:8080/products
```

## Stopping the Application

To stop all running services, use the following command:
```bash
docker compose down
```
This will stop and remove the containers and the network created by `docker-compose up`.
