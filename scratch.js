const API = "https://backend-ysf3.onrender.com";

async function debug() {
  const adminLogin = await fetch(`${API}/auth/login`, {
    method:'POST', headers:{'Content-Type':'application/json'},
    body: JSON.stringify({email:'admin@vigilai.health', password:'Admin@123'})
  }).then(r => r.json());

  // Get all users to see their entityIds
  const users = await fetch(`${API}/api/admin/users`, {
    headers:{'Authorization':`Bearer ${adminLogin.token}`}
  }).then(r => r.json());

  users.forEach(u => {
    console.log(`${u.email} | role=${u.role} | entityId=${u.entityId} | active=${u.isActive}`);
  });

  // Now try vitals with hardcoded clinicId
  const clinicLogin = await fetch(`${API}/auth/login`, {
    method:'POST', headers:{'Content-Type':'application/json'},
    body: JSON.stringify({email:'clinic@vigilai.health', password:'Clinic@123'})
  }).then(r => r.json());

  console.log("\nFull clinic login response:", JSON.stringify(clinicLogin, null, 2));
  
  // Try submitting vitals with a known clinicId
  const res = await fetch(`${API}/api/clinic/vitals`, {
    method:'POST',
    headers:{'Content-Type':'application/json','Authorization':`Bearer ${clinicLogin.token}`},
    body: JSON.stringify({
      clinicId: 'clinic-demo-001', phoneNumber:'+919876543210',
      fullName:'Ramesh Patil', age:45, gender:'M',
      heart_rate:72, spo2:98, respiratory_rate:16,
      systolic_bp:120, diastolic_bp:80, temperature:36.8
    })
  });
  console.log("\nVitals status:", res.status);
  console.log("Vitals body:", await res.text());
}
debug();
