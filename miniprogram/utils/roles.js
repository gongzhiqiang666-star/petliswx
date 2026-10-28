function roleCodes(userInfo) {
  if (!userInfo) return []
  const roles = userInfo.roles || userInfo.roleCodes || userInfo.role || []
  return (Array.isArray(roles) ? roles : [roles]).map(role => String(role && (role.code || role.roleCode || role.role || role)).toUpperCase())
}

function isHospitalAdmin(userInfo) {
  return roleCodes(userInfo).includes('HOSPITAL_ADMIN')
}

async function ensureRoles(userInfo, loadRoles) {
  if (roleCodes(userInfo).length) return userInfo
  const roles = await loadRoles()
  return { ...(userInfo || {}), roles: roles.roles || roles }
}

module.exports = { roleCodes, isHospitalAdmin, ensureRoles }
