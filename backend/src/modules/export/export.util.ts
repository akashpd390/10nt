/**
 * @file export.util.ts
 * @module modules/export
 * @description Provides utilities to build CSV files and PDF invoices for tenants and monthly rolls.
 */

import { stringify } from 'csv-stringify/sync';
import PDFDocument from 'pdfkit';

export interface InvoicePdfData {
  tenantName: string;
  roomNo: string;
  billingMonth: string;
  rentAmount: number;
  prevReading: number;
  currReading: number;
  unitsConsumed: number;
  electricityRate: number;
  electricityAmount: number;
  totalDueAmount: number;
  previousBalance: number;
  finalBalance: number;
  generatedDate: string;
}

/**
 * Converts array of tabular objects into CSV string.
 * @param records Array of row objects.
 * @returns Formatted CSV text string.
 */
export async function generateCsvContent(records: Record<string, any>[]): Promise<string> {
  if (!records || records.length === 0) {
    return '';
  }

  return stringify(records, {
    header: true,
    columns: Object.keys(records[0]),
  });
}

/**
 * Creates an in-memory PDF invoice using PDFKit.
 * @param data Invoice parameters including rent, electricity, and signed balances.
 * @returns Buffer containing the rendered PDF document.
 */
export async function generateInvoicePdfBuffer(data: InvoicePdfData): Promise<Buffer> {
  return new Promise((resolve, reject) => {
    try {
      const doc = new PDFDocument({ margin: 40, size: 'A4' });
      const buffers: Buffer[] = [];

      doc.on('data', (chunk) => buffers.push(chunk));
      doc.on('end', () => resolve(Buffer.concat(buffers)));
      doc.on('error', (err) => reject(err));

      // Header
      doc.fontSize(20).text('10nts - PROPERTY MANAGEMENT', { align: 'center' });
      doc.moveDown(0.3);
      doc.fontSize(12).text('Tenant Monthly Statement & Invoice', { align: 'center' });
      doc.moveDown(1.5);

      // Metadata Box
      doc.fontSize(10);
      doc.text(`Tenant Name: ${data.tenantName}`);
      doc.text(`Room No: ${data.roomNo}`);
      doc.text(`Billing Month: ${data.billingMonth}`);
      doc.text(`Date of Issue: ${data.generatedDate}`);
      doc.moveDown(1);

      // Horizontal line
      doc.strokeColor('#cccccc').lineWidth(1).moveTo(40, doc.y).lineTo(550, doc.y).stroke();
      doc.moveDown(1);

      // Invoice Line Items
      doc.fontSize(11).text('Charges Breakdown:', { underline: true });
      doc.moveDown(0.5);

      doc.fontSize(10);
      doc.text(`1. Base Monthly Rent: Rs ${data.rentAmount.toFixed(2)}`);
      doc.text(
        `2. Electricity Units: ${data.unitsConsumed} units (Meter: ${data.prevReading} -> ${data.currReading} @ Rs ${data.electricityRate}/unit)`
      );
      doc.text(`   Electricity Charge: Rs ${data.electricityAmount.toFixed(2)}`);
      doc.moveDown(0.5);

      // Horizontal line
      doc.strokeColor('#eeeeee').lineWidth(1).moveTo(40, doc.y).lineTo(550, doc.y).stroke();
      doc.moveDown(0.5);

      doc.fontSize(11).text(`Current Month Total: Rs ${data.totalDueAmount.toFixed(2)}`, { bold: true } as any);
      doc.text(`Previous Balance / Advance: Rs ${data.previousBalance.toFixed(2)}`);
      doc.moveDown(0.5);

      // Final Balance Box
      const balanceType =
        data.finalBalance < 0
          ? `Total Due (Arrears): Rs ${Math.abs(data.finalBalance).toFixed(2)}`
          : data.finalBalance > 0
          ? `Advance Credit Remaining: Rs ${data.finalBalance.toFixed(2)}`
          : 'Settled (Rs 0.00)';

      doc.fontSize(12).text(`Net Balance: ${balanceType}`, { underline: true });
      doc.moveDown(2);

      // Footer
      doc.fontSize(9).text('This is a computer generated invoice. No signature required.', { align: 'center' });

      doc.end();
    } catch (err) {
      reject(err);
    }
  });
}
