const API = "https://backend-ysf3.onrender.com";

async function test() {
  const loginRes = await fetch(`${API}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'admin@vigilai.health', password: 'Admin@123' })
  });
  const loginData = await loginRes.json();
  const token = loginData.token;

  const res = await fetch(`${API}/api/admin/audit`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  
  if (!res.ok) {
    console.log("Audit failed:", res.status);
    console.log(await res.text());
  } else {
    console.log("Audit OK:", await res.json());
  }
}
test();
