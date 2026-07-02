# TicketApp - Initial State & Configuration

This document explains how to configure the project and use the
`initial-state.txt` initialization mechanism.

------------------------------------------------------------------------

# Configuration (`application.properties`)

## `spring.datasource.url`

The PostgreSQL connection URL.

``` properties
spring.datasource.url=jdbc:postgresql://136.115.146.17:5432/ticketapp_db?connectTimeout=5
```

Defines the database server, port, database name and connection timeout.

------------------------------------------------------------------------

## `spring.datasource.username`

Database username.

``` properties
spring.datasource.username=ticketapp_user
```

------------------------------------------------------------------------

## `spring.datasource.password`

Database password.

``` properties
spring.datasource.password=BGUticketapp1!
```

------------------------------------------------------------------------

## `repository.type`

Determines which repository implementation is used.

``` properties
repository.type=DB
```

Options:

-   `DB` -- PostgreSQL
-   `MEMORY` -- In-memory repositories

------------------------------------------------------------------------

## `initial.state.file`

The initialization script that will be executed when the application
starts.

``` properties
initial.state.file=initial-state.txt
```

------------------------------------------------------------------------

## `initial.state.enabled`

Enables or disables the initialization mechanism.

``` properties
initial.state.enabled=true
```

------------------------------------------------------------------------

## `initial.state.reset`

Deletes existing data before loading the initialization file.

``` properties
initial.state.reset=true
```

------------------------------------------------------------------------

## `initial.state.skipExisting`

Skips commands that create existing objects.

``` properties
initial.state.skipExisting=true
```

------------------------------------------------------------------------

## `initial.state.exitOnFailure`

Stops the application if initialization fails.

``` properties
initial.state.exitOnFailure=true
```

------------------------------------------------------------------------

## `initial.state.createAdmin`

Creates the default administrator automatically before executing the
initialization file.

``` properties
initial.state.createAdmin=true
```

------------------------------------------------------------------------

## `admin.name`

Administrator username.

``` properties
admin.name=admin
```

------------------------------------------------------------------------

## `admin.passward`

Administrator password.

``` properties
admin.passward=admin
```

When `initial.state.createAdmin=true`, these credentials can be used to
log into the system.

------------------------------------------------------------------------

## `external.system.url`

URL of the external system used by the application.

``` properties
external.system.url=https://damp-lynna-wsep-1984852e.koyeb.app/
```

------------------------------------------------------------------------

## `logging.file.path`

Directory where application logs are stored.

``` properties
logging.file.path=logs
```

------------------------------------------------------------------------

# Initial State Commands

The initialization file is located under:

``` text
src/main/resources/initial-state.txt
```

Each line represents one command.

General format:

``` text
command argument1 argument2 ...
```

Lines beginning with `#` and empty lines are ignored.

------------------------------------------------------------------------

## register

Creates a new user.

``` text
register username password age email
```

Example:

``` text
register roi 1234 25 roi@gmail.com
```

------------------------------------------------------------------------

## login

Logs into an existing user.

``` text
login username password
```

Example:

``` text
login roi 1234
```

All following commands are executed using the currently logged-in user.

------------------------------------------------------------------------

## logout

Logs out the current user.

``` text
logout
```

------------------------------------------------------------------------

## create-company

Creates a new company.

``` text
create-company companyName
```

Example:

``` text
create-company BGU_Events
```

------------------------------------------------------------------------

## appoint-manager

Appoints a manager.

``` text
appoint-manager managerUsername companyName permissions
```

Example:

``` text
appoint-manager david BGU_Events CREATE_EVENT,EDIT_EVENT
```

------------------------------------------------------------------------

## approve-manager

Approves a manager appointment.

``` text
approve-manager companyName
```

------------------------------------------------------------------------

## appoint-owner

Appoints a new owner.

``` text
appoint-owner ownerUsername companyName
```

------------------------------------------------------------------------

## approve-owner

Approves an owner appointment.

``` text
approve-owner companyName
```

------------------------------------------------------------------------

## create-event

Creates a new event.

``` text
create-event companyName eventName artistName type price location daysFromNow standing seats
```

Example:

``` text
create-event BGU_Events Coldplay_Show Coldplay MUSIC 350 Beer_Sheva 30 200 800
```

Parameter description:

  Parameter     Description
  ------------- --------------------------------
  companyName   Company name
  eventName     Event name
  artistName    Artist name
  type          Event type
  price         Ticket price
  location      Event location
  daysFromNow   Number of days until the event
  standing      Number of standing tickets
  seats         Number of seated tickets

------------------------------------------------------------------------

## configure-lottery

Creates a lottery configuration.

``` text
configure-lottery companyName eventName minutesFromNow maxWinners
```

------------------------------------------------------------------------

## discount-simple

``` text
discount-simple targetId targetType percentage companyName saveAs
```

Creates a simple percentage discount.

------------------------------------------------------------------------

## discount-quantity

``` text
discount-quantity targetId targetType percentage minQuantity companyName saveAs
```

Creates a quantity-based discount.

------------------------------------------------------------------------

## discount-coupon

``` text
discount-coupon targetId targetType couponCode percentage companyName saveAs
```

Creates a coupon discount.

------------------------------------------------------------------------

## discount-sum

``` text
discount-sum targetId targetType companyName saveAs id1,id2,id3
```

Combines multiple discounts into a sum policy.

------------------------------------------------------------------------

## discount-max

``` text
discount-max targetId targetType companyName saveAs id1,id2,id3
```

Combines multiple discounts using the maximum discount.

------------------------------------------------------------------------

## policy-age

``` text
policy-age targetId targetType minimumAge saveAs
```

Creates an age restriction policy.

------------------------------------------------------------------------

## policy-quantity

``` text
policy-quantity targetId targetType minimumQuantity maximumQuantity saveAs
```

Creates a quantity restriction policy.

------------------------------------------------------------------------

## policy-and

``` text
policy-and targetId targetType saveAs id1,id2,id3
```

Combines policies using logical AND.

------------------------------------------------------------------------

## policy-or

``` text
policy-or targetId targetType saveAs id1,id2,id3
```

Combines policies using logical OR.

------------------------------------------------------------------------
# Documentation And Relevant files

``` text
link to UML: https://drive.google.com/file/d/1hMbAo6SjcIWjn3nkTz5ZGisPiODqYpbd/view?usp=sharing
```

``` text
link to Use Case File: https://drive.google.com/file/d/11HMMY3Lb8KsHFuJ9TAof66p5-6wX0eaG/view?usp=sharing
```
