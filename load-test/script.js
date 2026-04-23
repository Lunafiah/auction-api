import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '10s', target: 50 },  // Ramp up to 50 virtual users
        { duration: '30s', target: 50 },  // Stay at 50 users for 30s
        { duration: '10s', target: 100 }, // Spike to 100 users
        { duration: '30s', target: 100 }, // Stay at 100
        { duration: '10s', target: 0 },   // Ramp down to 0
    ],
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export function setup() {
    // We create a user for testing
    const uniqueUsername = `loadtester_${Math.floor(Math.random() * 1000000)}`;
    const payload = JSON.stringify({
        username: uniqueUsername,
        password: 'password123',
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const res = http.post(`${BASE_URL}/api/auth/register`, payload, params);
    
    // Fallback if user already exists
    if (res.status !== 200) {
        const loginRes = http.post(`${BASE_URL}/api/auth/login`, payload, params);
        return { token: loginRes.json('token') };
    }

    return { token: res.json('token') };
}

export default function (data) {
    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${data.token}`,
        },
    };

    // We assume an auction with ID 1 exists. We will try to place bids.
    const randomBid = Math.floor(Math.random() * 1000) + 100;
    const bidPayload = JSON.stringify({
        amount: randomBid
    });

    const res = http.post(`${BASE_URL}/api/auctions/1/bid`, bidPayload, params);

    check(res, {
        'is status 200 or 409': (r) => r.status === 200 || r.status === 409, // 200 OK or 409 Conflict (Optimistic Lock)
        'is status 500': (r) => r.status === 500,
    });

    sleep(1);
}
