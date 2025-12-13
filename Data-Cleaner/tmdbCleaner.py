import pandas as pd
import numpy as np  # We need numpy to handle "Not a Number" (NaN) correctly

print("--- Starting the Advanced Cleaning Process ---")

input_filename = "MovieData_1100.csv"
output_filename = "MovieData_Typed.csv"

# 1. LOAD DATA
try:
    df = pd.read_csv(input_filename)
    print(f"Loaded {len(df)} rows.")
except FileNotFoundError:
    print(f"Error: {input_filename} not found.")
    exit()


# 2. MOJIBAKE FIXER (Your text cleaner)
def fix_mojibake(text):
    if not isinstance(text, str):
        return text
    try:
        return text.encode('latin-1').decode('utf-8')
    except Exception:
        return text


# 3. NUMBER CLEANER (New!)
# This function turns "$1,000,000" or "N/A" into a clean number like 1000000.0
def clean_money_or_score(value):
    # If it's already empty, return 0
    if pd.isna(value) or str(value).strip() == "N/A":
        return 0

    # Make it a string so we can manipulate it
    str_val = str(value)

    # Remove '$', ',', '%', and spaces
    clean_str = str_val.replace('$', '').replace(',', '').replace('%', '').replace(' ', '')

    try:
        # Convert to a decimal number (float)
        return float(clean_str)
    except ValueError:
        return 0  # If it's still garbage, return 0


# --- APPLYING THE LOGIC ---

print("Step 1: Fixing Text Characters (Mojibake)...")
text_cols = ['Movie Name', 'Genre', 'Actors', 'Director']
for col in text_cols:
    if col in df.columns:
        df[col] = df[col].apply(fix_mojibake)
        df[col] = df[col].astype(str)  # Force it to be a String type

print("Step 2: Cleaning Release Year...")
# We remove non-numbers, turn "N/A" to 0, and make it an Integer.
if 'Release Year' in df.columns:
    # Coerce errors means "if you find text, turn it into NaN"
    df['Release Year'] = pd.to_numeric(df['Release Year'], errors='coerce').fillna(0).astype(int)

print("Step 3: Cleaning Numbers (Score, Budget, Revenue)...")
# We want these to be numbers so the Database accepts them.
number_cols = ['User Score', 'Budget', 'Revenue']
for col in number_cols:
    if col in df.columns:
        # Apply our 'clean_money_or_score' function
        df[col] = df[col].apply(clean_money_or_score)

        # Determine specific type:
        # Score is usually an Integer (e.g. 76)
        # Money can be huge, so we usually use Float or Int (Int64)
        if col == 'User Score':
            df[col] = df[col].astype(int)
        else:
            df[col] = df[col].astype(float)  # Budget/Revenue as Float

# --- FINAL CHECK ---
print("\nData Types after cleaning:")
print(df.dtypes)

# 4. SAVE
# Note: When saving to CSV, floats might look like 10000.0
# This is perfect for Databases.
df.to_csv(output_filename, index=False, encoding='utf-8-sig')

print("------------------------------------------------")
print(f"Done! Saved as {output_filename}")
print("Your data is now Database-Ready (Clean numbers, no symbols).")
print("------------------------------------------------")