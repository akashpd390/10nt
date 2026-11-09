CREATE TYPE "public"."bill_status" AS ENUM('UNPAID', 'PARTIALLY_PAID', 'PAID');--> statement-breakpoint
CREATE TYPE "public"."ledger_type" AS ENUM('BILL_CHARGE', 'PAYMENT_RECEIVED', 'MANUAL_ADJUSTMENT');--> statement-breakpoint
CREATE TYPE "public"."payment_mode" AS ENUM('CASH', 'UPI', 'BANK_TRANSFER');--> statement-breakpoint
CREATE TABLE "bills" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"tenant_id" uuid NOT NULL,
	"billing_month" varchar(7) NOT NULL,
	"rent_amount" numeric(12, 2) NOT NULL,
	"prev_meter_reading" numeric(12, 2) NOT NULL,
	"curr_meter_reading" numeric(12, 2) NOT NULL,
	"units_consumed" numeric(12, 2) NOT NULL,
	"electricity_rate_per_unit" numeric(8, 2) NOT NULL,
	"electricity_amount" numeric(12, 2) NOT NULL,
	"total_due_amount" numeric(12, 2) NOT NULL,
	"status" "bill_status" DEFAULT 'UNPAID' NOT NULL,
	"generated_at" timestamp with time zone DEFAULT now() NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "ledger_entries" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"tenant_id" uuid NOT NULL,
	"type" "ledger_type" NOT NULL,
	"amount" numeric(12, 2) NOT NULL,
	"running_balance" numeric(12, 2) NOT NULL,
	"description" text NOT NULL,
	"reference_id" uuid,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "payments" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"tenant_id" uuid NOT NULL,
	"amount_paid" numeric(12, 2) NOT NULL,
	"payment_mode" "payment_mode" DEFAULT 'CASH' NOT NULL,
	"payment_date" timestamp with time zone DEFAULT now() NOT NULL,
	"notes" text,
	"balance_before" numeric(12, 2) NOT NULL,
	"balance_after" numeric(12, 2) NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "rooms" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"room_no" varchar(50) NOT NULL,
	"description" text,
	"floor" integer DEFAULT 0 NOT NULL,
	"attachments" text[],
	"base_price" numeric(12, 2) DEFAULT '0.00' NOT NULL,
	"is_occupied" boolean DEFAULT false NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	"updated_at" timestamp with time zone DEFAULT now() NOT NULL,
	CONSTRAINT "rooms_room_no_unique" UNIQUE("room_no")
);
--> statement-breakpoint
CREATE TABLE "tenants" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"name" varchar(255) NOT NULL,
	"phone" varchar(20) NOT NULL,
	"family_members_count" integer DEFAULT 1 NOT NULL,
	"room_id" uuid,
	"allotted_price" numeric(12, 2) NOT NULL,
	"electricity_rate_per_unit" numeric(8, 2) DEFAULT '8.00' NOT NULL,
	"last_meter_reading" numeric(12, 2) DEFAULT '0.00' NOT NULL,
	"adhaar_card_url" text,
	"profile_pic_url" text,
	"current_balance" numeric(12, 2) DEFAULT '0.00' NOT NULL,
	"is_active" boolean DEFAULT true NOT NULL,
	"joined_date" timestamp with time zone DEFAULT now() NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	"updated_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "users" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"email" varchar(255) NOT NULL,
	"password_hash" varchar(255) NOT NULL,
	"refresh_token_hash" varchar(255),
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	"updated_at" timestamp with time zone DEFAULT now() NOT NULL,
	CONSTRAINT "users_email_unique" UNIQUE("email")
);
--> statement-breakpoint
ALTER TABLE "bills" ADD CONSTRAINT "bills_tenant_id_tenants_id_fk" FOREIGN KEY ("tenant_id") REFERENCES "public"."tenants"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "ledger_entries" ADD CONSTRAINT "ledger_entries_tenant_id_tenants_id_fk" FOREIGN KEY ("tenant_id") REFERENCES "public"."tenants"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "payments" ADD CONSTRAINT "payments_tenant_id_tenants_id_fk" FOREIGN KEY ("tenant_id") REFERENCES "public"."tenants"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "tenants" ADD CONSTRAINT "tenants_room_id_rooms_id_fk" FOREIGN KEY ("room_id") REFERENCES "public"."rooms"("id") ON DELETE set null ON UPDATE no action;