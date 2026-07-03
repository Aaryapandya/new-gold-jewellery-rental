#!/usr/bin/env bash
set -e
BASE="/Users/aarya.pandya1/Documents/Gold Jewellery Rental/new-gold-jewellery-rental"
OUT="$BASE/src/main/java/com/goldrental/service/JewelleryService.java"
touch "$OUT"
echo "Script ready - run: bash scripts/gen_jewellery_service.sh"
