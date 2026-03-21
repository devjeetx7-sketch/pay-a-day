const fs = require('fs');

let code = fs.readFileSync('src/pages/Login.tsx', 'utf-8');

console.log(code);
