export const directoryOptions = (users) => users.map((user) => ({
  value: user.id,
  label: `${user.displayName || user.username}（${user.username} · ID ${user.id}）`
}));

export const itemOptions = (items) => items.map((item) => ({
  value: item.id,
  label: `${item.name}（${item.code} · ID ${item.id}）`
}));

export const hospitalOptions = (hospitals) => hospitals.map((hospital) => ({
  value: hospital.id,
  label: `${hospital.name}（${hospital.code} · ID ${hospital.id}）`
}));
