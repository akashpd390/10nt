ALTER TABLE "rooms" DROP CONSTRAINT "rooms_room_no_unique";--> statement-breakpoint
ALTER TABLE "rooms" ADD COLUMN "landlord_id" uuid;--> statement-breakpoint
ALTER TABLE "tenants" ADD COLUMN "landlord_id" uuid;--> statement-breakpoint
ALTER TABLE "rooms" ADD CONSTRAINT "rooms_landlord_id_users_id_fk" FOREIGN KEY ("landlord_id") REFERENCES "public"."users"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "tenants" ADD CONSTRAINT "tenants_landlord_id_users_id_fk" FOREIGN KEY ("landlord_id") REFERENCES "public"."users"("id") ON DELETE cascade ON UPDATE no action;