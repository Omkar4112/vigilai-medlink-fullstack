const API = "https://backend-ysf3.onrender.com";

async function test() {
  const loginRes = await fetch(`${API}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'admin@vigilai.health', password: 'Admin@123' })
  });
  const loginData = await loginRes.json();
  const token = loginData.token;

  console.log("Sending repair request...");
  
  while (true) {
    try {
      const res = await fetch(`${API}/api/admin/audit/repair`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (res.ok) {
         console.log("REPAIRED SUCCESSFULLY:", await res.text());
         break;
      } else {
         console.log("Failed:", res.status);
      }
    } catch (e) {
      console.error("Fetch error:", e.message);
    }
    await new Promise(r => setTimeout(r, 10000));
  }
}
test();
