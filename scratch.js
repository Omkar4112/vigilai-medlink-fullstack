const API = "https://backend-ysf3.onrender.com";

async function test() {
  const loginRes = await fetch(`${API}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'admin@vigilai.health', password: 'Admin@123' })
  });
  const loginData = await loginRes.json();
  const token = loginData.token;

  const res = await fetch(`${API}/api/admin/audit?page=0&size=50`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  
  const data = await res.json();
  const logs = data.content;
  
  logs.forEach(l => {
    if (l.logId >= 33) {
      console.log(`Log ${l.logId}: curr=${l.hashCurrent.substring(0,8)} prev=${l.hashPrevious.substring(0,8)} action=${l.action}`);
    }
  });
}
test();
