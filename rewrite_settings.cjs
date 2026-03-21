const fs = require('fs');

let code = fs.readFileSync('src/pages/SettingsPage.tsx', 'utf-8');

// We want to replace the entire <div className="flex flex-col md:flex-row gap-6"> to the end of its block with our new responsive layout.
// I will just use regex or string split.
const [head, body] = code.split('<div className="flex flex-col md:flex-row gap-6">');
const tail = body.substring(body.lastIndexOf('</div>\n        </div>\n      </div>')); // actually let's use a simpler marker.

// We will recreate the render method entirely.
