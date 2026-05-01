const API = "https://backend-ysf3.onrender.com";

async function debug() {
  // Check clinic login
  const clinicRes = await fetch(`${API}/auth/login`, {
    method: 'POST', headers: {'Content-Type':'application/json'},
    body: JSON.stringify({email:'clinic@vigilai.health', password:'Admin@123'})
  });
  console.log("Clinic status:", clinicRes.status);
  const clinicText = await clinicRes.text();
  console.log("Clinic body:", clinicText);

  // Check hospital login
  const hospRes = await fetch(`${API}/auth/login`, {
    method: 'POST', headers: {'Content-Type':'application/json'},
    body: JSON.stringify({email:'hospital@vigilai.health', password:'Admin@123'})
  });
  console.log("\nHospital status:", hospRes.status);
  const hospText = await hospRes.text();
  console.log("Hospital body:", hospText);

  // Check clinic login with Clinic@123
  const clinicRes2 = await fetch(`${API}/auth/login`, {
    method: 'POST', headers: {'Content-Type':'application/json'},
    body: JSON.stringify({email:'clinic@vigilai.health', password:'Clinic@123'})
  });
  console.log("\nClinic with Clinic@123:", clinicRes2.status);
  const clinicText2 = await clinicRes2.text();
  console.log("Body:", clinicText2);

  // Check hospital with Hospital@123
  const hospRes2 = await fetch(`${API}/auth/login`, {
    method: 'POST', headers: {'Content-Type':'application/json'},
    body: JSON.stringify({email:'hospital@vigilai.health', password:'Hospital@123'})
  });
  console.log("\nHospital with Hospital@123:", hospRes2.status);
  const hospText2 = await hospRes2.text();
  console.log("Body:", hospText2);
}
debug();
