const fs = require('node:fs')
const path = require('node:path')
const { execFileSync } = require('node:child_process')
const root = path.resolve(__dirname, '..')
function walk(dir) { return fs.readdirSync(dir,{withFileTypes:true}).flatMap(entry => entry.isDirectory() ? walk(path.join(dir,entry.name)) : [path.join(dir,entry.name)]) }
const files = walk(root).filter(file => !file.includes(`${path.sep}node_modules${path.sep}`) && !file.includes(`${path.sep}tests${path.sep}`))
for (const file of files) {
  if (file.endsWith('.js')) execFileSync(process.execPath,['--check',file],{stdio:'pipe'})
  if (file.endsWith('.json')) JSON.parse(fs.readFileSync(file,'utf8'))
}
console.log(`JS/JSON检查通过：${files.filter(x=>x.endsWith('.js')).length} 个JS，${files.filter(x=>x.endsWith('.json')).length} 个JSON`)
