# SMS Permission Notice

Telesis requests `READ_SMS` and `RECEIVE_SMS` only for private local parsing of bank, UPI, card, wallet, and ATM transaction messages. The app intentionally declares no `INTERNET` permission and is intended for personal sideloaded use, not public Play Store distribution.

Raw SMS bodies are parsed on-device. Persistent transaction records store parsed financial fields plus deduplication hashes rather than full raw SMS bodies.
