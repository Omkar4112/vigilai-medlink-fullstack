const API = "https://backend-ysf3.onrender.com";

async function fullTest() {
  console.log("=== VIGILAI MEDLINK FULL A-Z TEST ===\n");
  const start = Date.now();
  const af = (url, token, opts={}) => fetch(url, {...opts, headers:{'Content-Type':'application/json','Authorization':`Bearer ${token}`,...(opts.headers||{})}});
  const results = [];
  const test = async (n, name, fn) => {
    const t = Date.now();
    try {
      const r = await fn();
      const ms = Date.now()-t;
      results.push({n, name, ok:true, ms});
      console.log(`${n}. ${name}... ✅ (${ms}ms) ${r||''}`);
    } catch(e) {
      const ms = Date.now()-t;
      results.push({n, name, ok:false, ms});
      console.log(`${n}. ${name}... ❌ (${ms}ms) ${e.message}`);
    }
  };

  // Wait for new deployment
  console.log("Waiting for new deployment...");
  while (true) {
    try {
      const r = await fetch(`${API}/auth/login`, {
        method:'POST', headers:{'Content-Type':'application/json'},
        body: JSON.stringify({email:'clinic@vigilai.health', password:'Clinic@123'})
      }).then(r => r.json());
      if (r.entityId) { console.log("New deployment detected! entityId =", r.entityId, "\n"); break; }
    } catch(e) {}
    process.stdout.write(".");
    await new Promise(r => setTimeout(r, 10000));
  }

  // 1-4: Auth
  await test(1, "Health", async () => { await fetch(`${API}/health`).then(r=>r.json()); });
  
  let adminToken, clinicToken, hospToken, clinicEntity, hospEntity;
  await test(2, "Admin login", async () => {
    const r = await fetch(`${API}/auth/login`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email:'admin@vigilai.health',password:'Admin@123'})}).then(r=>r.json());
    adminToken = r.token; return `role=${r.role}`;
  });
  await test(3, "Clinic login", async () => {
    const r = await fetch(`${API}/auth/login`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email:'clinic@vigilai.health',password:'Clinic@123'})}).then(r=>r.json());
    clinicToken = r.token; clinicEntity = r.entityId; return `role=${r.role} entityId=${r.entityId}`;
  });
  await test(4, "Hospital login", async () => {
    const r = await fetch(`${API}/auth/login`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email:'hospital@vigilai.health',password:'Hospital@123'})}).then(r=>r.json());
    hospToken = r.token; hospEntity = r.entityId; return `role=${r.role} entityId=${r.entityId}`;
  });

  // 5-9: Admin
  await test(5, "Admin dashboard", async () => {
    const d = await af(`${API}/api/admin/dashboard`,adminToken).then(r=>r.json());
    return `users=${d.totalUsers} hosp=${d.totalHospitals} docs=${d.totalDoctors} alerts=${d.totalAlerts}`;
  });
  await test(6, "Admin hospitals", async () => { const d = await af(`${API}/api/admin/hospitals`,adminToken).then(r=>r.json()); return `${d.length} hospitals`; });
  await test(7, "Admin users", async () => { const d = await af(`${API}/api/admin/users`,adminToken).then(r=>r.json()); return `${d.length} users`; });
  await test(8, "Audit logs", async () => { const d = await af(`${API}/api/admin/audit?page=0&size=5`,adminToken).then(r=>r.json()); return `${d.totalElements} logs`; });
  await test(9, "Integrity verify", async () => { const d = await af(`${API}/api/admin/audit/verify`,adminToken,{method:'POST'}).then(r=>r.json()); return d.status; });

  // 10-12: Clinic
  await test(10, "Clinic patients", async () => { const r = await af(`${API}/api/clinic/patients?clinicId=${clinicEntity}`,clinicToken); if(!r.ok) throw new Error(r.status); const d = await r.json(); return `${d.length} patients`; });
  await test(11, "Clinic dashboard", async () => { const r = await af(`${API}/api/clinic/patients/dashboard?clinicId=${clinicEntity}`,clinicToken); if(!r.ok) throw new Error(r.status); return 'OK'; });
  await test(12, "Clinic alerts", async () => { const r = await af(`${API}/api/clinic/alerts?clinicId=${clinicEntity}`,clinicToken); if(!r.ok) throw new Error(r.status); const d = await r.json(); return `${d.length} alerts`; });

  // 13-15: Hospital
  await test(13, "Hospital dashboard", async () => {
    const d = await af(`${API}/api/hospital/dashboard?hospitalId=${hospEntity}`,hospToken).then(r=>r.json());
    return `icu=${d.icuAvailable}/${d.icuTotal} docs=${d.totalDoctors} pending=${d.pendingAlerts}`;
  });
  await test(14, "Hospital doctors", async () => { const d = await af(`${API}/api/hospital/doctors?hospitalId=${hospEntity}`,hospToken).then(r=>r.json()); return `${d.length} doctors`; });
  await test(15, "Hospital alerts", async () => { const d = await af(`${API}/api/hospital/alerts`,hospToken).then(r=>r.json()); return `${d.length} pending`; });

  // 16: Unified
  await test(16, "Unified summary", async () => { const d = await af(`${API}/api/dashboard/summary`,adminToken).then(r=>r.json()); return `vitals=${d.vitalsToday} icu=${d.icuAvailable} status=${d.systemStatus}`; });

  // 17: Vitals
  await test(17, "Submit vitals", async () => {
    const d = await af(`${API}/api/clinic/vitals`,clinicToken,{method:'POST',body:JSON.stringify({
      clinicId:clinicEntity, phoneNumber:'+919876543210', fullName:'Ramesh Patil', age:45, gender:'M',
      heart_rate:72, spo2:98, respiratory_rate:16, systolic_bp:120, diastolic_bp:80, temperature:36.8
    })}).then(r=>r.json());
    return `risk=${d.riskLevel} score=${d.riskScore} triage=${d.triageSeverity}`;
  });

  // 18: Search
  await test(18, "Patient search", async () => { const d = await af(`${API}/api/clinic/patients/search?clinicId=${clinicEntity}&query=Ramesh`,clinicToken).then(r=>r.json()); return `${d.length} results`; });

  // Summary
  const total = Date.now()-start;
  const passed = results.filter(r=>r.ok).length;
  const avgMs = Math.round(results.reduce((a,r)=>a+r.ms,0)/results.length);
  console.log(`\n${'='.repeat(55)}`);
  console.log(`  ${passed}/18 PASSED | Total: ${total}ms | Avg: ${avgMs}ms`);
  console.log(`${'='.repeat(55)}`);
}
fullTest();
