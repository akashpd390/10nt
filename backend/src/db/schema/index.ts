/**
 * @file index.ts
 * @module db/schema
 * @description Central export file for all Drizzle PostgreSQL database schemas.
 * Includes users, rooms, tenants, bills, payments, and ledger entries.
 */

import { pgTable, uuid, varchar, text, integer, numeric, boolean, timestamp, pgEnum } from 'drizzle-orm/pg-core';
import { relations } from 'drizzle-orm';

// Enums
export const paymentModeEnum = pgEnum('payment_mode', ['CASH', 'UPI', 'BANK_TRANSFER']);
export const billStatusEnum = pgEnum('bill_status', ['UNPAID', 'PARTIALLY_PAID', 'PAID']);
export const ledgerTypeEnum = pgEnum('ledger_type', ['BILL_CHARGE', 'PAYMENT_RECEIVED', 'MANUAL_ADJUSTMENT']);

/**
 * Users Table (Landlord Admin)
 */
export const users = pgTable('users', {
  id: uuid('id').defaultRandom().primaryKey(),
  email: varchar('email', { length: 255 }).notNull().unique(),
  passwordHash: varchar('password_hash', { length: 255 }).notNull(),
  refreshTokenHash: varchar('refresh_token_hash', { length: 255 }),
  createdAt: timestamp('created_at', { withTimezone: true }).defaultNow().notNull(),
  updatedAt: timestamp('updated_at', { withTimezone: true }).defaultNow().notNull(),
});

/**
 * Rooms Table
 */
export const rooms = pgTable('rooms', {
  id: uuid('id').defaultRandom().primaryKey(),
  roomNo: varchar('room_no', { length: 50 }).notNull().unique(),
  description: text('description'),
  floor: integer('floor').notNull().default(0),
  attachments: text('attachments').array(), // e.g. ["kitchen", "attached_bathroom", "balcony"]
  basePrice: numeric('base_price', { precision: 12, scale: 2 }).notNull().default('0.00'),
  isOccupied: boolean('is_occupied').notNull().default(false),
  createdAt: timestamp('created_at', { withTimezone: true }).defaultNow().notNull(),
  updatedAt: timestamp('updated_at', { withTimezone: true }).defaultNow().notNull(),
});

/**
 * Tenants Table
 */
export const tenants = pgTable('tenants', {
  id: uuid('id').defaultRandom().primaryKey(),
  name: varchar('name', { length: 255 }).notNull(),
  phone: varchar('phone', { length: 20 }).notNull(),
  familyMembersCount: integer('family_members_count').notNull().default(1),
  roomId: uuid('room_id').references(() => rooms.id, { onDelete: 'set null' }),
  allottedPrice: numeric('allotted_price', { precision: 12, scale: 2 }).notNull(),
  electricityRatePerUnit: numeric('electricity_rate_per_unit', { precision: 8, scale: 2 }).notNull().default('8.00'),
  lastMeterReading: numeric('last_meter_reading', { precision: 12, scale: 2 }).notNull().default('0.00'),
  adhaarCardUrl: text('adhaar_card_url'),
  profilePicUrl: text('profile_pic_url'),
  // Signed balance: < 0 is overdue/arrears, 0 is settled, > 0 is advance prepaid credit
  currentBalance: numeric('current_balance', { precision: 12, scale: 2 }).notNull().default('0.00'),
  isActive: boolean('is_active').notNull().default(true),
  joinedDate: timestamp('joined_date', { withTimezone: true }).defaultNow().notNull(),
  createdAt: timestamp('created_at', { withTimezone: true }).defaultNow().notNull(),
  updatedAt: timestamp('updated_at', { withTimezone: true }).defaultNow().notNull(),
});

/**
 * Bills Table (Monthly rent and electricity computations)
 */
export const bills = pgTable('bills', {
  id: uuid('id').defaultRandom().primaryKey(),
  tenantId: uuid('tenant_id').references(() => tenants.id, { onDelete: 'cascade' }).notNull(),
  billingMonth: varchar('billing_month', { length: 7 }).notNull(), // Format: YYYY-MM
  rentAmount: numeric('rent_amount', { precision: 12, scale: 2 }).notNull(),
  prevMeterReading: numeric('prev_meter_reading', { precision: 12, scale: 2 }).notNull(),
  currMeterReading: numeric('curr_meter_reading', { precision: 12, scale: 2 }).notNull(),
  unitsConsumed: numeric('units_consumed', { precision: 12, scale: 2 }).notNull(),
  electricityRatePerUnit: numeric('electricity_rate_per_unit', { precision: 8, scale: 2 }).notNull(),
  electricityAmount: numeric('electricity_amount', { precision: 12, scale: 2 }).notNull(),
  totalDueAmount: numeric('total_due_amount', { precision: 12, scale: 2 }).notNull(),
  status: billStatusEnum('status').notNull().default('UNPAID'),
  generatedAt: timestamp('generated_at', { withTimezone: true }).defaultNow().notNull(),
  createdAt: timestamp('created_at', { withTimezone: true }).defaultNow().notNull(),
});

/**
 * Payments Table (Manual landlord payment receipts)
 */
export const payments = pgTable('payments', {
  id: uuid('id').defaultRandom().primaryKey(),
  tenantId: uuid('tenant_id').references(() => tenants.id, { onDelete: 'cascade' }).notNull(),
  amountPaid: numeric('amount_paid', { precision: 12, scale: 2 }).notNull(),
  paymentMode: paymentModeEnum('payment_mode').notNull().default('CASH'),
  paymentDate: timestamp('payment_date', { withTimezone: true }).defaultNow().notNull(),
  notes: text('notes'),
  balanceBefore: numeric('balance_before', { precision: 12, scale: 2 }).notNull(),
  balanceAfter: numeric('balance_after', { precision: 12, scale: 2 }).notNull(),
  createdAt: timestamp('created_at', { withTimezone: true }).defaultNow().notNull(),
});

/**
 * Ledger Entries Table (Double-entry / audit history of balance mutations)
 */
export const ledgerEntries = pgTable('ledger_entries', {
  id: uuid('id').defaultRandom().primaryKey(),
  tenantId: uuid('tenant_id').references(() => tenants.id, { onDelete: 'cascade' }).notNull(),
  type: ledgerTypeEnum('type').notNull(),
  amount: numeric('amount', { precision: 12, scale: 2 }).notNull(),
  runningBalance: numeric('running_balance', { precision: 12, scale: 2 }).notNull(),
  description: text('description').notNull(),
  referenceId: uuid('reference_id'), // ID of associated Bill or Payment
  createdAt: timestamp('created_at', { withTimezone: true }).defaultNow().notNull(),
});

// Relations
export const roomsRelations = relations(rooms, ({ one }) => ({
  tenant: one(tenants, {
    fields: [rooms.id],
    references: [tenants.roomId],
  }),
}));

export const tenantsRelations = relations(tenants, ({ one, many }) => ({
  room: one(rooms, {
    fields: [tenants.roomId],
    references: [rooms.id],
  }),
  bills: many(bills),
  payments: many(payments),
  ledgerEntries: many(ledgerEntries),
}));

export const billsRelations = relations(bills, ({ one }) => ({
  tenant: one(tenants, {
    fields: [bills.tenantId],
    references: [tenants.id],
  }),
}));

export const paymentsRelations = relations(payments, ({ one }) => ({
  tenant: one(tenants, {
    fields: [payments.tenantId],
    references: [tenants.id],
  }),
}));

export const ledgerEntriesRelations = relations(ledgerEntries, ({ one }) => ({
  tenant: one(tenants, {
    fields: [ledgerEntries.tenantId],
    references: [tenants.id],
  }),
}));
