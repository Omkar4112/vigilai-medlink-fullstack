const fs = require('fs');
const file = 'c:/Users/omkar/modified-vigilai-medlink/frontend/admin.html';
let data = fs.readFileSync(file, 'utf8');

// Remove dead loadHospitals function
data = data.replace(/async function loadHospitals\(\) \{[\s\S]*?^\}/m, '');

// Remove dead registerHospital function
data = data.replace(/async function registerHospital\(\) \{[\s\S]*?^\}/m, '');

// Remove dead deleteHospital function
data = data.replace(/async function deleteHospital\(id\) \{[\s\S]*?^\}/m, '');

// Remove loadDocs() call
data = data.replace(/\nloadDocs\(\);\n/, '\n');

fs.writeFileSync(file, data, 'utf8');
console.log('Done cleaning dead code');
