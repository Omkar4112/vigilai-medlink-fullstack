const API = "https://backend-ysf3.onrender.com";

async function test() {
  const loginRes = await fetch(`${API}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'hospital@vigilai.health', password: 'Hospital@123' })
  });
  const loginData = await loginRes.json();
  const token = loginData.token;

  const sumRes = await fetch(`${API}/api/dashboard/summary`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  
  if (!sumRes.ok) {
    console.log("Summary failed:", sumRes.status);
    console.log(await sumRes.text());
  } else {
    console.log("Summary OK:", await sumRes.json());
  }
}
test();
