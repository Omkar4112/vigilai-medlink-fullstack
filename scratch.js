const fs = require('fs');
const file = 'c:/Users/omkar/modified-vigilai-medlink/frontend/admin.html';
let data = fs.readFileSync(file, 'utf8');

const newUsersSection = `      <div class="panel">
        <div class="panel-header" style="margin-bottom:8px">
          <h3>👥 System Users</h3>
          <button class="btn-sm" onclick="loadUsers()">↻ Refresh</button>
        </div>
        
        <h4 style="font-size:12px;color:var(--accent);margin-bottom:8px;margin-top:16px;border-bottom:1px solid var(--border);padding-bottom:4px">🏥 Clinics</h4>
        <table>
          <thead><tr><th>Email</th><th>Name</th><th>Entity ID</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody id="clinicUsersTbody"><tr><td colspan="5" style="text-align:center;color:var(--muted);padding:16px">Loading...</td></tr></tbody>
        </table>

        <h4 style="font-size:12px;color:#4da6ff;margin-bottom:8px;margin-top:24px;border-bottom:1px solid var(--border);padding-bottom:4px">🏨 Hospitals</h4>
        <table>
          <thead><tr><th>Email</th><th>Name</th><th>Entity ID</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody id="hospitalUsersTbody"><tr><td colspan="5" style="text-align:center;color:var(--muted);padding:16px">Loading...</td></tr></tbody>
        </table>

        <h4 style="font-size:12px;color:var(--yellow);margin-bottom:8px;margin-top:24px;border-bottom:1px solid var(--border);padding-bottom:4px">🛡️ System Admins</h4>
        <table>
          <thead><tr><th>Email</th><th>Name</th><th>Entity ID</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody id="adminUsersTbody"><tr><td colspan="5" style="text-align:center;color:var(--muted);padding:16px">Loading...</td></tr></tbody>
        </table>
      </div>`;

data = data.replace(/<div class="panel">\s*<div class="panel-header">\s*<h3>👥 System Users<\/h3>\s*<button class="btn-sm" onclick="loadUsers\(\)">↻ Refresh<\/button>\s*<\/div>\s*<table>\s*<thead>.*?<\/table>\s*<\/div>/s, newUsersSection);

const newLoadUsers = `async function loadUsers() {
  try {
    const res  = await authFetch(\`\${API}/api/admin/users\`);
    const data = await res.json();
    
    const clinics = data.filter(u => u.role === 'CLINIC');
    const hospitals = data.filter(u => u.role === 'HOSPITAL');
    const admins = data.filter(u => u.role === 'ADMIN');

    const renderRows = (users) => users.map(u => \`
      <tr>
        <td style="font-family:var(--mono)">\${u.email}</td>
        <td>\${u.fullName||'—'}</td>
        <td style="font-family:var(--mono);font-size:11px;color:var(--muted)">\${u.entityId||'—'}</td>
        <td><span class="status-badge \${u.isActive?'sb-active':'sb-inactive'}">\${u.isActive?'ACTIVE':'INACTIVE'}</span></td>
        <td><button class="btn-sm">Edit</button></td>
      </tr>\`).join('') || '<tr><td colspan="5" style="text-align:center;padding:16px;color:var(--muted);font-size:12px">No users in this category</td></tr>';

    document.getElementById('clinicUsersTbody').innerHTML = renderRows(clinics);
    document.getElementById('hospitalUsersTbody').innerHTML = renderRows(hospitals);
    document.getElementById('adminUsersTbody').innerHTML = renderRows(admins);
  } catch {
    document.getElementById('clinicUsersTbody').innerHTML = \`<tr><td colspan="5" style="text-align:center;padding:16px;color:var(--muted);font-size:12px">Demo Clinic</td></tr>\`;
    document.getElementById('hospitalUsersTbody').innerHTML = \`<tr><td colspan="5" style="text-align:center;padding:16px;color:var(--muted);font-size:12px">Demo Hospital</td></tr>\`;
    document.getElementById('adminUsersTbody').innerHTML = \`<tr><td colspan="5" style="text-align:center;padding:16px;color:var(--muted);font-size:12px">System Admin</td></tr>\`;
  }
}`;

data = data.replace(/async function loadUsers\(\) \{[\s\S]*?\}\s*(?=\nasync function addUser\(\))/s, newLoadUsers + '\n');

fs.writeFileSync(file, data, 'utf8');
