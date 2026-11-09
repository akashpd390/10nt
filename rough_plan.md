
Project 10nt but for protect the folder convention we use alterative name tenant

description : this is the tenants management project which follows the monolith architcure same backend and mobile app in single dir. 

techstack:
    backend: nodejs , express js , typescript , nodemon etc for the development db: neon db , jwt, clean layered feature based architecture, drizzle orm, refresh access token arch where session on mobile device upto 7 days opena api docs clean docs, deploy on free tier render server, object storage with cloudinary 

    mobile: jetpack compose, koin for depency injection , retrofit , kotlin, viemodel , simple mobile architecture proper handling the strings and api endpoints obsfuction , localistaion , sperations of concerns etc 



flow: this is the simple teneant mangemnt app where the user only me no one so it just an personal app to managee mine tenants, 

this is following that manner login , auth, there is rooms, tenants , manual payment entry as form, so i can mange the payment history one place also an option to export the data in csv and pdf like tenants payments , tenants bills, tenants whole history invoices etc


features: 
    1. login /auth [not register here i will handle this in backend side ] can have fearture to update the password using the previos password right now no otp or anything else simple login 
    2. Room: 
        Room no, description , floot,attachments like kitchen or batthroom  (optional) , minimum ammount of this, create update remove , with proper are you sure dialog screen

    3. Tenants:
        id, name, no. family memebers, room id of assign room , room price or alloted price, electricity bill defailt to 8 rs per units, contact details and two optional tenant id like adhaar or profile pic, create , update, remove, with proper are u sure screen if the tenant has the pending balance

    4. payments (immportant): 
        there is no payment interagation i want it just an simply manaul entry , id, slect tenants proper dropdown suggestion to pick from the available tenants and search on that list, on select there should be profile pic show if available in db and i info button to details page, previos unpaid electryicyt bill unit then rent ammount editable, electricity bill , to current date of the payment in unit calculated by the rs/unit and enum like cash or upi , or cleaner way to handle the balance and advace ammount like (i want sugesion from u how to handle this i am thingking of it like two wasy advace payment , balcne or +ve and -ve balance becuase sometime tenant give extra amount any month and say adjust it in future monthly cycle ) , current electrricyt unit , to calculate the electryicity bill , pay ammount , payment date, and after submitint payment there is option which should send the sms text message to that tenant contact from it details to send sms using the sendsmsm intent premmision i will come to this later but this is the important feature, 


    5. dashboard: 
        - room occupancy wrt total available rooms, tenants and total members or peoples, total balance pending , list of the tenants who didnt pay the current month , update this list every month cycle , also show the which tenants have to pay how much ammount this month in listile with larger the amount soriting , list of transaction last 5 , 

    6. useful screens: 
        tenants list, update, remove, room list, with update, remove, 

    



critical design system approaches confustion need discussion from u:

    1. sms i want it local first sms approach using only the mobile sms sim feature to send the sms alerts with an button to notify the updated payments, 
    2. exports to pdf and csv, invocices of the payments, tenants, elctricyty bill including , like month wise for all tenants tenants wise to all 5 months etc
    3. and a global loader like whenver we facing the timeout on api but the internet is avalibel we assume that render server is went into the 15 min inactive sleep m,ode to wake this app it takes a time so we handle it cleanly whenver we encournter this case with all cases we hit the check helth api continusoly 10 times untill we get the respone without timeout if we found success true we fire in app notifications which should fire on the even if the app is close to remebering me that sertver is alive again u can continue the app using the foregrouds service eg flow. 
        flow :
            suppose after long time i open the app the apps is fails check token becuse of the timeout becuase server is sleep mode and it staring i cant wait that long so i close the app in that case this foregorund sevice fire and handle it cleanly and on scucess it fire the notifiction to rember me that app is start u can open the app  als this notification should not come when the app is in focus . 

        

the critical points need to discuss

