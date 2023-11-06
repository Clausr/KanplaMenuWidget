# KanplaMenuWidget
To use this code, make a copy of `secrets.defaults.properties` and insert your own bearer token

```bash
cp secrets.defaults.properties secrets.properties
```

Your `moduleID` can be found on the kanpla webpage, by looking at the network requests in the developer terminal; look for it in the paylod of "allowances" or "orders"

## Features / TODO
- [ ] Widget
  - [x] Show todays menu
  - [ ] Be able to have different widgets with different configurations

- [ ] Configuration screen
  - [x] Set module id
  - [x] Set preferred menu
  - [ ] Show all days horizontally?
  - [ ] Show tomorrows menu after this time of day (e.g. after 12:00)
  - [ ] Weekend states: 
    - [ ] Hourly picture of cats 😻
    - [x] Show monday menu
    - [ ] Transparent mode?

- [ ] Get the actual menu that personally selected for the day (This requires a new endpoint)
