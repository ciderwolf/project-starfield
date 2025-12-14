package starfield.data.dao

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.*
import org.jetbrains.exposed.sql.transactions.experimental.*
import starfield.Config
import starfield.data.table.*

object DatabaseSingleton {
    fun init() {
        val config = HikariConfig()
        config.jdbcUrl = Config.connectionString
        config.minimumIdle = 3
        val dataSource = HikariDataSource(config)
        val database = Database.connect(dataSource)
        transaction(database) {
            SchemaUtils.create(Cards, Tokens, Users, Decks, DeckCards, CardSources, CardExtras, Printings, Cubes, CubeCards, DraftSets, CardParts)
            
            registerManaFunctions()
        }
    }

    private  fun exec(sql: String) {
        TransactionManager.current().exec(sql)
    }

    private fun registerManaFunctions() {
        // Extract generic (numeric) mana from a mana cost
        exec(
            """
            CREATE OR REPLACE FUNCTION mana_generic(mana_cost VARCHAR) 
            RETURNS INTEGER AS $$
            DECLARE
                symbols TEXT[];
                symbol TEXT;
                generic_total INTEGER := 0;
            BEGIN
                IF mana_cost IS NULL OR mana_cost = '' THEN
                    RETURN 0;
                END IF;
                
                symbols := string_to_array(regexp_replace(mana_cost, '[{}]', ' ', 'g'), ' ');
                
                FOREACH symbol IN ARRAY symbols
                LOOP
                    IF symbol != '' THEN
                        BEGIN
                            generic_total := generic_total + symbol::INTEGER;
                        EXCEPTION WHEN OTHERS THEN
                            -- Not a number, skip
                        END;
                    END IF;
                END LOOP;
                
                RETURN generic_total;
            END;
            $$ LANGUAGE plpgsql IMMUTABLE;
        """.trimIndent())
        
        // Extract colored symbols as a sorted string
        exec(
            """
            CREATE OR REPLACE FUNCTION mana_colored(mana_cost VARCHAR) 
            RETURNS TEXT AS $$
            DECLARE
                symbols TEXT[];
                symbol TEXT;
                colored_symbols TEXT[] := ARRAY[]::TEXT[];
            BEGIN
                IF mana_cost IS NULL OR mana_cost = '' THEN
                    RETURN '';
                END IF;
                
                symbols := string_to_array(regexp_replace(mana_cost, '[{}]', ' ', 'g'), ' ');
                
                FOREACH symbol IN ARRAY symbols
                LOOP
                    IF symbol != '' THEN
                        BEGIN
                            PERFORM symbol::INTEGER;
                        EXCEPTION WHEN OTHERS THEN
                            colored_symbols := array_append(colored_symbols, symbol);
                        END;
                    END IF;
                END LOOP;
                
                -- Sort and concatenate
                SELECT string_agg(s, '' ORDER BY s) INTO symbol FROM unnest(colored_symbols) s;
                RETURN COALESCE(symbol, '');
            END;
            $$ LANGUAGE plpgsql IMMUTABLE;
        """.trimIndent())
        
        // Check if container contains all symbols in contained (strict or non-strict)
        exec(
            """
            CREATE OR REPLACE FUNCTION mana_colored_contains(container TEXT, contained TEXT, strict BOOLEAN) 
            RETURNS BOOLEAN AS $$
            DECLARE
                container_chars TEXT[];
                contained_chars TEXT[];
                char TEXT;
                remaining TEXT;
            BEGIN
                IF contained IS NULL OR contained = '' THEN
                    RETURN NOT strict OR (container IS NOT NULL AND container != '');
                END IF;
                
                IF container IS NULL OR container = '' THEN
                    RETURN FALSE;
                END IF;
                
                -- Convert strings to character arrays
                container_chars := string_to_array(container, NULL);
                contained_chars := string_to_array(contained, NULL);
                remaining := container;
                
                -- Check if all contained chars exist in container
                FOREACH char IN ARRAY contained_chars
                LOOP
                    IF position(char in remaining) = 0 THEN
                        RETURN FALSE;
                    END IF;
                    -- Remove first occurrence
                    remaining := overlay(remaining placing '' from position(char in remaining) for 1);
                END LOOP;
                
                -- If strict, must have leftover symbols
                IF strict THEN
                    RETURN length(remaining) > 0;
                END IF;
                
                RETURN TRUE;
            END;
            $$ LANGUAGE plpgsql IMMUTABLE;
        """.trimIndent())
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}