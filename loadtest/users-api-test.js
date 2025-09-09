import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');

// Test configuration
export const options = {
  stages: [
    { duration: '30s', target: 10 },   // Ramp up to 10 users over 30s
    { duration: '1m', target: 50 },    // Stay at 50 users for 1m
    { duration: '30s', target: 100 },  // Ramp up to 100 users over 30s
    { duration: '2m', target: 100 },   // Stay at 100 users for 2m
    { duration: '30s', target: 0 },    // Ramp down to 0 users over 30s
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% of requests must complete below 500ms
    http_req_failed: ['rate<0.05'],   // Error rate must be below 5%
    errors: ['rate<0.1'],             // Custom error rate must be below 10%
  },
};

// Single port - Vert.x handles request distribution across verticles automatically
const BASE_URL = 'http://localhost:8080';

// Test data for creating users
const testUsers = [
  { name: 'John Doe', email: 'john@example.com' },
  { name: 'Jane Smith', email: 'jane@example.com' },
  { name: 'Bob Johnson', email: 'bob@example.com' },
  { name: 'Alice Brown', email: 'alice@example.com' },
  { name: 'Charlie Wilson', email: 'charlie@example.com' },
];

export function setup() {
  // Health check before starting the test
  const healthResponse = http.get(`${BASE_URL}/health`);
  console.log(`Health check status: ${healthResponse.status}`);
  
  if (healthResponse.status !== 200) {
    throw new Error('Application is not healthy, aborting test');
  }
  
  return { baseUrl: BASE_URL };
}

export default function (data) {
  const baseUrl = data.baseUrl;
  
  // Test scenario weights (random selection)
  const scenarios = [
    { weight: 40, name: 'getAllUsers' },
    { weight: 30, name: 'getUserById' },
    { weight: 15, name: 'createUser' },
    { weight: 10, name: 'updateUser' },
    { weight: 5, name: 'deleteUser' },
  ];
  
  const random = Math.random() * 100;
  let cumulativeWeight = 0;
  let selectedScenario = 'getAllUsers';
  
  for (const scenario of scenarios) {
    cumulativeWeight += scenario.weight;
    if (random <= cumulativeWeight) {
      selectedScenario = scenario.name;
      break;
    }
  }
  
  // Execute selected scenario - randomly choose between Users and Orders
  const isOrderScenario = Math.random() < 0.3; // 30% orders, 70% users
  
  if (isOrderScenario) {
    switch (selectedScenario) {
      case 'getAllUsers':
        testGetAllOrders(baseUrl);
        break;
      case 'getUserById':
        testGetOrderById(baseUrl);
        break;
      case 'createUser':
        testCreateOrder(baseUrl);
        break;
      case 'updateUser':
        testUpdateOrder(baseUrl);
        break;
      case 'deleteUser':
        testDeleteOrder(baseUrl);
        break;
    }
  } else {
    switch (selectedScenario) {
      case 'getAllUsers':
        testGetAllUsers(baseUrl);
        break;
      case 'getUserById':
        testGetUserById(baseUrl);
        break;
      case 'createUser':
        testCreateUser(baseUrl);
        break;
      case 'updateUser':
        testUpdateUser(baseUrl);
        break;
      case 'deleteUser':
        testDeleteUser(baseUrl);
        break;
    }
  }
  
  // Random sleep between requests (0.1-1 second)
  sleep(Math.random() * 0.9 + 0.1);
}

function testGetAllUsers(baseUrl) {
  const response = http.get(`${baseUrl}/api/users`);
  
  const result = check(response, {
    'GET /api/users status is 200': (r) => r.status === 200,
    'GET /api/users response time < 500ms': (r) => r.timings.duration < 500,
    'GET /api/users has valid JSON': (r) => {
      try {
        JSON.parse(r.body);
        return true;
      } catch (e) {
        return false;
      }
    },
  });
  
  errorRate.add(!result);
}

function testGetUserById(baseUrl) {
  // Use random user ID (1 or 2 from initial data, or recently created)
  const userId = Math.random() < 0.7 ? (Math.random() < 0.5 ? '1' : '2') : String(Date.now());
  
  const response = http.get(`${baseUrl}/api/users/${userId}`);
  
  const result = check(response, {
    'GET /api/users/:id status is 200 or 404': (r) => r.status === 200 || r.status === 404,
    'GET /api/users/:id response time < 300ms': (r) => r.timings.duration < 300,
  });
  
  if (response.status === 200) {
    check(response, {
      'GET /api/users/:id has valid user JSON': (r) => {
        try {
          const user = JSON.parse(r.body);
          return user.id && user.name && user.email;
        } catch (e) {
          return false;
        }
      },
    });
  }
  
  errorRate.add(!result);
}

function testCreateUser(baseUrl) {
  const testUser = testUsers[Math.floor(Math.random() * testUsers.length)];
  const uniqueUser = {
    name: `${testUser.name}_${Date.now()}`,
    email: `${Date.now()}_${testUser.email}`,
  };
  
  const response = http.post(
    `${baseUrl}/api/users`,
    JSON.stringify(uniqueUser),
    { headers: { 'Content-Type': 'application/json' } }
  );
  
  const result = check(response, {
    'POST /api/users status is 201': (r) => r.status === 201,
    'POST /api/users response time < 800ms': (r) => r.timings.duration < 800,
    'POST /api/users returns created user': (r) => {
      try {
        const user = JSON.parse(r.body);
        return user.id && user.name === uniqueUser.name && user.email === uniqueUser.email;
      } catch (e) {
        return false;
      }
    },
  });
  
  errorRate.add(!result);
}

function testUpdateUser(baseUrl) {
  // Try to update user 1 or 2
  const userId = Math.random() < 0.5 ? '1' : '2';
  const updatedUser = {
    name: `Updated User ${Date.now()}`,
    email: `updated_${Date.now()}@example.com`,
  };
  
  const response = http.put(
    `${baseUrl}/api/users/${userId}`,
    JSON.stringify(updatedUser),
    { headers: { 'Content-Type': 'application/json' } }
  );
  
  const result = check(response, {
    'PUT /api/users/:id status is 200 or 404': (r) => r.status === 200 || r.status === 404,
    'PUT /api/users/:id response time < 600ms': (r) => r.timings.duration < 600,
  });
  
  errorRate.add(!result);
}

function testDeleteUser(baseUrl) {
  // Create a user first, then delete it
  const tempUser = {
    name: `Temp User ${Date.now()}`,
    email: `temp_${Date.now()}@example.com`,
  };
  
  const createResponse = http.post(
    `${baseUrl}/api/users`,
    JSON.stringify(tempUser),
    { headers: { 'Content-Type': 'application/json' } }
  );
  
  if (createResponse.status === 201) {
    const createdUser = JSON.parse(createResponse.body);
    
    const deleteResponse = http.del(`${baseUrl}/api/users/${createdUser.id}`);
    
    const result = check(deleteResponse, {
      'DELETE /api/users/:id status is 200': (r) => r.status === 200,
      'DELETE /api/users/:id response time < 400ms': (r) => r.timings.duration < 400,
    });
    
    errorRate.add(!result);
  }
}

// Order API test functions
function testGetAllOrders(baseUrl) {
  const response = http.get(`${baseUrl}/api/orders`);
  
  const result = check(response, {
    'GET /api/orders status is 200': (r) => r.status === 200,
    'GET /api/orders response time < 500ms': (r) => r.timings.duration < 500,
    'GET /api/orders has valid JSON': (r) => {
      try {
        JSON.parse(r.body);
        return true;
      } catch (e) {
        return false;
      }
    },
  });
  
  errorRate.add(!result);
}

function testGetOrderById(baseUrl) {
  const orderId = Math.random() < 0.7 ? (Math.random() < 0.5 ? 'order-1' : 'order-2') : `order-${Date.now()}`;
  
  const response = http.get(`${baseUrl}/api/orders/${orderId}`);
  
  const result = check(response, {
    'GET /api/orders/:id status is 200 or 404': (r) => r.status === 200 || r.status === 404,
    'GET /api/orders/:id response time < 300ms': (r) => r.timings.duration < 300,
  });
  
  if (response.status === 200) {
    check(response, {
      'GET /api/orders/:id has valid order JSON': (r) => {
        try {
          const order = JSON.parse(r.body);
          return order.id && order.customerId && order.items;
        } catch (e) {
          return false;
        }
      },
    });
  }
  
  errorRate.add(!result);
}

function testCreateOrder(baseUrl) {
  const testOrder = {
    customerId: `customer-${Math.floor(Math.random() * 1000)}`,
    items: [
      {
        productId: `prod-${Math.floor(Math.random() * 100)}`,
        productName: `Product ${Math.floor(Math.random() * 100)}`,
        quantity: Math.floor(Math.random() * 5) + 1,
        unitPrice: (Math.random() * 100 + 10).toFixed(2)
      }
    ]
  };
  
  const response = http.post(
    `${baseUrl}/api/orders`,
    JSON.stringify(testOrder),
    { headers: { 'Content-Type': 'application/json' } }
  );
  
  const result = check(response, {
    'POST /api/orders status is 201': (r) => r.status === 201,
    'POST /api/orders response time < 800ms': (r) => r.timings.duration < 800,
    'POST /api/orders returns created order': (r) => {
      try {
        const order = JSON.parse(r.body);
        return order.id && order.customerId === testOrder.customerId;
      } catch (e) {
        return false;
      }
    },
  });
  
  errorRate.add(!result);
}

function testUpdateOrder(baseUrl) {
  const orderId = Math.random() < 0.5 ? 'order-1' : 'order-2';
  const updatedOrder = {
    customerId: `updated-customer-${Date.now()}`,
    items: [
      {
        productId: 'updated-prod-1',
        productName: 'Updated Product',
        quantity: 2,
        unitPrice: '49.99'
      }
    ]
  };
  
  const response = http.put(
    `${baseUrl}/api/orders/${orderId}`,
    JSON.stringify(updatedOrder),
    { headers: { 'Content-Type': 'application/json' } }
  );
  
  const result = check(response, {
    'PUT /api/orders/:id status is 200 or 404': (r) => r.status === 200 || r.status === 404,
    'PUT /api/orders/:id response time < 600ms': (r) => r.timings.duration < 600,
  });
  
  errorRate.add(!result);
}

function testDeleteOrder(baseUrl) {
  // Create a temporary order first, then delete it
  const tempOrder = {
    customerId: `temp-customer-${Date.now()}`,
    items: [
      {
        productId: 'temp-prod',
        productName: 'Temp Product',
        quantity: 1,
        unitPrice: '9.99'
      }
    ]
  };
  
  const createResponse = http.post(
    `${baseUrl}/api/orders`,
    JSON.stringify(tempOrder),
    { headers: { 'Content-Type': 'application/json' } }
  );
  
  if (createResponse.status === 201) {
    const createdOrder = JSON.parse(createResponse.body);
    
    const deleteResponse = http.del(`${baseUrl}/api/orders/${createdOrder.id}`);
    
    const result = check(deleteResponse, {
      'DELETE /api/orders/:id status is 200': (r) => r.status === 200,
      'DELETE /api/orders/:id response time < 400ms': (r) => r.timings.duration < 400,
    });
    
    errorRate.add(!result);
  }
}

export function teardown(data) {
  console.log('Load test completed');
}