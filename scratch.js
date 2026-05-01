const API = "https://backend-ysf3.onrender.com";

async function test() {
  const loginRes = await fetch(`${API}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'hospital@vigilai.health', password: 'Hospital@123' })
  });
  const loginData = await loginRes.json();
  const token = loginData.token;

  const res = await fetch(`${API}/api/hospital/dashboard?hospitalId=1`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  
  if (!res.ok) {
    console.log("Dashboard failed:", res.status);
  } else {
    console.log("Dashboard OK:", await res.json());
  }
}
test();
