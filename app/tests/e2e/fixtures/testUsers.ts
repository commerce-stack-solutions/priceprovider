export interface TestUser {
  username: string;
  password: string;
  role: string;
  fullName?: string;
  email?: string;
}

export const adminUser: TestUser = {
  username: 'admin',
  password: 'admin123',
  role: 'admin',
  fullName: 'Admin User',
  email: 'admin@priceprovider.com',
};

export const regularUser: TestUser = {
  username: 'user',
  password: 'user123',
  role: 'user',
  fullName: 'Regular User',
  email: 'user@priceprovider.com',
};

export const invalidUser: TestUser = {
  username: 'admin',
  password: 'wrongpassword',
  role: 'invalid',
};

export const lockedUser: TestUser = {
  username: 'locked',
  password: 'locked123',
  role: 'locked',
};

export const emptyUser: TestUser = {
  username: '',
  password: '',
  role: 'guest',
};

export function getUserByRole(role: string): TestUser {
  const users: Record<string, TestUser> = {
    admin: adminUser,
    user: regularUser,
    invalid: invalidUser,
    locked: lockedUser,
    empty: emptyUser,
  };
  return users[role] || regularUser;
}

export function getAllTestUsers(): TestUser[] {
  return [adminUser, regularUser, invalidUser, lockedUser, emptyUser];
}