/**
 * @file seed.ts
 * @module db/seed
 * @description Seeds initial landlord, dummy rooms, tenants, bills, payments, and ledger records.
 */

import { db } from './index';
import { users, rooms, tenants, payments, ledgerEntries } from './schema';
import { hashPassword } from '../modules/auth/auth.service';
import { env } from '../config/env';
import { eq } from 'drizzle-orm';

async function seed() {
  console.log('🌱 Starting seed...');

  // 1. Ensure Landlord Admin
  let [landlord] = await db.select().from(users).where(eq(users.email, env.ADMIN_EMAIL)).limit(1);
  if (!landlord) {
    const passwordHash = await hashPassword(env.ADMIN_DEFAULT_PASSWORD);
    const [created] = await db
      .insert(users)
      .values({
        email: env.ADMIN_EMAIL,
        passwordHash,
      })
      .returning();
    landlord = created;
    console.log(`👤 Created Landlord: ${landlord.email}`);
  } else {
    console.log(`👤 Using existing Landlord: ${landlord.email}`);
  }

  // 2. Clear previous dummy rooms/tenants for clean demonstration
  await db.delete(rooms).where(eq(rooms.landlordId, landlord.id));

  // 3. Create Sample Rooms
  const [room101] = await db
    .insert(rooms)
    .values({
      landlordId: landlord.id,
      roomNo: '101',
      description: '1BHK with Attached Kitchen & Bathroom',
      floor: 1,
      basePrice: '5500.00',
      isOccupied: true,
    })
    .returning();

  const [room102] = await db
    .insert(rooms)
    .values({
      landlordId: landlord.id,
      roomNo: '102',
      description: 'Single Room with Balcony',
      floor: 1,
      basePrice: '4000.00',
      isOccupied: true,
    })
    .returning();

  const [room201] = await db
    .insert(rooms)
    .values({
      landlordId: landlord.id,
      roomNo: '201',
      description: '2BHK Spacious Family Flat',
      floor: 2,
      basePrice: '8500.00',
      isOccupied: false,
    })
    .returning();

  console.log('🚪 Created 3 Sample Rooms (101, 102, 201)');

  // 4. Create Sample Tenants
  // Tenant 1: Has overdue balance of ₹2,500
  const [tenant1] = await db
    .insert(tenants)
    .values({
      landlordId: landlord.id,
      roomId: room101.id,
      name: 'Rahul Sharma',
      phone: '9876543210',
      familyMembersCount: 2,
      allottedPrice: '5500.00',
      electricityRatePerUnit: '8.00',
      lastMeterReading: '180.00',
      currentBalance: '-2500.00', // Due
      isActive: true,
    })
    .returning();

  // Tenant 2: Has advance credit of ₹1,000
  const [tenant2] = await db
    .insert(tenants)
    .values({
      landlordId: landlord.id,
      roomId: room102.id,
      name: 'Pooja Verma',
      phone: '9876543211',
      familyMembersCount: 1,
      allottedPrice: '4000.00',
      electricityRatePerUnit: '8.00',
      lastMeterReading: '120.00',
      currentBalance: '1000.00', // Advance
      isActive: true,
    })
    .returning();

  console.log('👥 Created 2 Sample Tenants (Rahul Sharma with dues, Pooja Verma with advance)');

  // 5. Add Sample Payments and Ledger Entries
  const [payment1] = await db
    .insert(payments)
    .values({
      tenantId: tenant1.id,
      amountPaid: '3000.00',
      paymentMode: 'UPI',
      notes: 'Partial payment for November',
      balanceBefore: '-5500.00',
      balanceAfter: '-2500.00',
    })
    .returning();

  await db.insert(ledgerEntries).values({
    tenantId: tenant1.id,
    type: 'PAYMENT_RECEIVED',
    amount: '3000.00',
    runningBalance: '-2500.00',
    description: 'UPI payment received (Transaction ID: UPI98234)',
    referenceId: payment1.id,
  });

  const [payment2] = await db
    .insert(payments)
    .values({
      tenantId: tenant2.id,
      amountPaid: '5000.00',
      paymentMode: 'CASH',
      notes: 'Advance rent payment',
      balanceBefore: '-4000.00',
      balanceAfter: '1000.00',
    })
    .returning();

  await db.insert(ledgerEntries).values({
    tenantId: tenant2.id,
    type: 'PAYMENT_RECEIVED',
    amount: '5000.00',
    runningBalance: '1000.00',
    description: 'Cash payment received (Advance)',
    referenceId: payment2.id,
  });

  console.log('✅ Seed completed successfully with demo records!');
  process.exit(0);
}

seed().catch((err) => {
  console.error('❌ Seed failed:', err);
  process.exit(1);
});
