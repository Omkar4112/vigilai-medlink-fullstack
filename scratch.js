const fs = require('fs');
const file = 'c:/Users/omkar/modified-vigilai-medlink/frontend/admin.html';
let data = fs.readFileSync(file, 'utf8');

const newLoadHospitalOverview = `async function loadHospitalOverview() {
  try {
    const res  = await authFetch(\`\${API}/api/admin/users\`);
    const data = await res.json();
    const hospitals = data.filter(u => u.role === 'HOSPITAL');
    
    if (hospitals.length === 0) {
      document.getElementById('hospitalOverview').innerHTML = '<div style="color:var(--muted);padding:16px;font-family:var(--mono);text-align:center">No hospitals registered yet. Create one in the Users tab.</div>';
      return;
    }

    document.getElementById('hospitalOverview').innerHTML = hospitals.slice(0,4).map(h => \`
      <div class="hospital-card">
        <div class="hospital-info">
          <div class="name">\${h.fullName || h.email}</div>
          <div class="meta">ID: \${h.entityId || 'N/A'} | Status: \${h.isActive ? 'ONLINE' : 'OFFLINE'}</div>
        </div>
        <div class="hospital-stats">
          <div class="hs-item">
            <div class="val" style="color:var(--accent2)">Active</div>
            <div class="lbl">Account</div>
          </div>
          <div class="hs-item">
            <div class="val" style="color:var(--muted2)">\${h.role}</div>
            <div class="lbl">Type</div>
          </div>
        </div>
      </div>\`).join('');
  } catch {
    document.getElementById('hospitalOverview').innerHTML = \`
      <div class="hospital-card">
        <div class="hospital-info"><div class="name">Demo Hospital</div><div class="meta">ID: 1</div></div>
        <div class="hospital-stats"><div class="hs-item"><div class="val" style="color:var(--accent2)">Active</div><div class="lbl">Account</div></div></div>
      </div>\`;
  }
}`;

data = data.replace(/async function loadHospitalOverview\(\) \{[\s\S]*?\}\s*(?=\nasync function loadRecentActivity\(\))/s, newLoadHospitalOverview + '\n');

fs.writeFileSync(file, data, 'utf8');
