/**
 * @file dashboard.controller.ts
 * @module modules/dashboard
 * @description Provides high-level dashboard metrics:
 * - Room occupancy vs total rooms
 * - Active tenants and total family members
 * - Total pending balance across all tenants
 * - Sorted list of unpaid tenants with due amounts
 * - Last 5 transactions
 */

import { Request, Response } from 'express';
import { db } from '../../db';
import { rooms, tenants, payments } from '../../db/schema';
import { eq, desc, sql } from 'drizzle-orm';

export async function getDashboardOverviewHandler(_req: Request, res: Response): Promise<void> {
  // 1. Room Occupancy
  const allRooms = await db.select().from(rooms);
  const totalRooms = allRooms.length;
  const occupiedRooms = allRooms.filter((r) => r.isOccupied).length;
  const availableRooms = totalRooms - occupiedRooms;

  // 2. Tenants & Family Members
  const allTenants = await db.query.tenants.findMany({
    where: eq(tenants.isActive, true),
    with: {
      room: true,
    },
  });

  const totalActiveTenants = allTenants.length;
  const totalPeopleCount = allTenants.reduce((sum, t) => sum + (t.familyMembersCount || 1), 0);

  // 3. Financial Balance Breakdown
  let totalPendingBalance = 0; // Total money tenants owe (< 0)
  let totalAdvanceBalance = 0; // Total advance money (> 0)

  // 4. Tenants with Dues (Sorted from largest due to smallest)
  const unpaidTenantsList: Array<{
    tenantId: string;
    tenantName: string;
    roomNo: string;
    phone: string;
    dueAmount: number;
    profilePicUrl: string | null;
  }> = [];

  for (const tenant of allTenants) {
    const bal = Number(tenant.currentBalance);
    if (bal < 0) {
      const due = Math.abs(bal);
      totalPendingBalance += due;
      unpaidTenantsList.push({
        tenantId: tenant.id,
        tenantName: tenant.name,
        roomNo: tenant.room?.roomNo || 'Unassigned',
        phone: tenant.phone,
        dueAmount: due,
        profilePicUrl: tenant.profilePicUrl,
      });
    } else if (bal > 0) {
      totalAdvanceBalance += bal;
    }
  }

  // Sort largest due first
  unpaidTenantsList.sort((a, b) => b.dueAmount - a.dueAmount);

  // 5. Recent 5 Transactions
  const recentPayments = await db.query.payments.findMany({
    with: {
      tenant: true,
    },
    orderBy: [desc(payments.paymentDate)],
    limit: 5,
  });

  const recentTransactions = recentPayments.map((p) => ({
    id: p.id,
    tenantName: p.tenant?.name || 'Unknown',
    amount: Number(p.amountPaid),
    mode: p.paymentMode,
    date: p.paymentDate,
    balanceAfter: Number(p.balanceAfter),
  }));

  res.status(200).json({
    occupancy: {
      totalRooms,
      occupiedRooms,
      availableRooms,
      occupancyPercentage: totalRooms > 0 ? Number(((occupiedRooms / totalRooms) * 100).toFixed(1)) : 0,
    },
    tenantsSummary: {
      totalActiveTenants,
      totalPeopleCount,
    },
    finances: {
      totalPendingBalance: Number(totalPendingBalance.toFixed(2)),
      totalAdvanceBalance: Number(totalAdvanceBalance.toFixed(2)),
    },
    unpaidTenants: unpaidTenantsList,
    recentTransactions,
  });
}
