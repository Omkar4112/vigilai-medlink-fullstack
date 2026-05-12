const fs = require('fs');
const file = 'c:/Users/omkar/modified-vigilai-medlink/frontend/admin.html';
let data = fs.readFileSync(file, 'utf8');
data = data.replace(/<div class="sb-item" onclick="showSection\('hospitals'\)">[\s\S]*?<\/div>/g, '');
data = data.replace(/<!-- Hospitals -->[\s\S]*?<!-- Audit Logs -->/g, '<!-- Audit Logs -->');
fs.writeFileSync(file, data, 'utf8');
