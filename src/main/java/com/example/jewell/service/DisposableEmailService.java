package com.example.jewell.service;

import org.springframework.stereotype.Service;
import java.util.Set;
import java.util.HashSet;

/**
 * Service to detect disposable/temporary email addresses.
 * These are often used for spam, fraud, or bypassing registration limits.
 */
@Service
public class DisposableEmailService {

    // Common disposable email domains - add more as needed
    private static final Set<String> DISPOSABLE_DOMAINS = new HashSet<>(Set.of(
        // Popular temporary email services
        "tempmail.com", "temp-mail.org", "tempmail.net", "temp-mail.io",
        "guerrillamail.com", "guerrillamail.org", "guerrillamail.net", "guerrillamail.biz",
        "mailinator.com", "mailinator.net", "mailinator.org",
        "10minutemail.com", "10minutemail.net", "10minmail.com",
        "throwaway.email", "throwawaymail.com",
        "fakeinbox.com", "fakemailgenerator.com",
        "getnada.com", "nada.email",
        "maildrop.cc", "mailnesia.com",
        "yopmail.com", "yopmail.fr", "yopmail.net",
        "trashmail.com", "trashmail.net", "trashmail.org",
        "dispostable.com", "disposableemailaddresses.com",
        "mailcatch.com", "meltmail.com",
        "mintemail.com", "mohmal.com",
        "mytrashmail.com", "sharklasers.com",
        "spam4.me", "spamgourmet.com",
        "tempinbox.com", "tempmailaddress.com",
        "temporaryemail.net", "temporarymail.org",
        "wegwerfmail.de", "wegwerfmail.net",
        "mailnull.com", "e4ward.com",
        "spamfree24.org", "spamherelots.com",
        "devnull.email", "mailslurp.com",
        "emkei.cz", "anonymbox.com",
        "binkmail.com", "bobmail.info",
        "cool.fr.nf", "courriel.fr.nf",
        "discard.email", "discardmail.com",
        "emailondeck.com", "emailsensei.com",
        "fakedemail.com", "fakemailgenerator.net",
        "getairmail.com", "imgof.com",
        "incognitomail.com", "jetable.fr.nf",
        "kasmail.com", "klassmaster.com",
        "mailexpire.com", "mailforspam.com",
        "mailin8r.com", "mailtemp.info",
        "nomail.xl.cx", "nospam.ze.tc",
        "owlpic.com", "proxymail.eu",
        "rcpt.at", "rejectmail.com",
        "rtrtr.com", "safe-mail.net",
        "sogetthis.com", "spamobox.com",
        "spamspot.com", "superrito.com",
        "teleworm.us", "tempemail.co.za",
        "tempmailer.com", "tempmailo.com",
        "tempomail.fr", "temporaryforwarding.com",
        "thanksnospam.info", "thisisnotmyrealemail.com",
        "tm2mail.com", "trash2009.com",
        "trbvm.com", "trickmail.net",
        "trillianpro.com", "uggsrock.com",
        "upliftnow.com", "uplipht.com",
        "venompen.com", "veryrealemail.com",
        "viditag.com", "viewcastmedia.com",
        "webm4il.info", "wh4f.org",
        "whyspam.me", "willselfdestruct.com",
        "winemaven.info", "wronghead.com",
        "wuzup.net", "wuzupmail.net",
        "xagloo.co", "xemaps.com",
        "xents.com", "xmaily.com",
        "xoxy.net", "yapped.net",
        "yet.net", "yuurok.com",
        "za.com", "zehnminutenmail.de",
        "zetmail.com", "zippymail.info",
        "zoaxe.com", "zoemail.org",
        "inboxkitten.com", "33mail.com",
        "burnermail.io", "emailfake.com"
    ));

    // Suspicious patterns in email local part
    private static final Set<String> SUSPICIOUS_PATTERNS = Set.of(
        "test", "fake", "spam", "trash", "temp", "disposable",
        "throwaway", "junk", "delete", "remove", "noreply",
        "asdf", "qwerty", "12345", "aaaaa", "xxxxx",
        "admin", "administrator", "root", "user", "info",
        "contact", "support", "hello", "hi", "abc", "xyz",
        "aaa", "bbb", "ccc", "ddd", "eee", "fff", "ggg",
        "sample", "example", "demo", "testing", "dummy"
    );

    // Valid common TLDs (top-level domains)
    private static final Set<String> VALID_TLDS = Set.of(
        "com", "org", "net", "edu", "gov", "mil", "io", "co",
        "in", "us", "uk", "ca", "au", "de", "fr", "jp", "cn",
        "ru", "br", "it", "es", "nl", "se", "no", "fi", "dk",
        "pl", "cz", "at", "ch", "be", "ie", "nz", "sg", "hk",
        "kr", "tw", "mx", "ar", "cl", "za", "ae", "sa", "il",
        "tr", "gr", "pt", "hu", "ro", "ua", "th", "my", "id",
        "ph", "vn", "pk", "bd", "ng", "ke", "eg", "ma", "info",
        "biz", "name", "pro", "mobi", "tel", "asia", "jobs",
        "travel", "museum", "aero", "coop", "cat", "xxx",
        "app", "dev", "page", "blog", "shop", "store", "online",
        "site", "website", "tech", "digital", "cloud", "email",
        "live", "me", "tv", "cc", "ws", "bz", "ly", "fm", "am",
        "ai", "gg", "to", "ac", "xyz"
    );

    // Suspicious/fake looking domain patterns
    private static final Set<String> SUSPICIOUS_DOMAINS_PATTERNS = Set.of(
        "ceo", "cto", "cfo", "vip", "boss", "chief", "owner",
        "rich", "money", "cash", "free", "win", "prize", "luck"
    );

    /**
     * Check if an email domain is known to be disposable.
     */
    public boolean isDisposableEmail(String email) {
        if (email == null || !email.contains("@")) {
            return false;
        }
        
        String domain = email.substring(email.lastIndexOf("@") + 1).toLowerCase();
        return DISPOSABLE_DOMAINS.contains(domain);
    }

    /**
     * Check if email has suspicious patterns (might be fake).
     */
    public boolean hasSuspiciousPattern(String email) {
        if (email == null || !email.contains("@")) {
            return false;
        }
        
        String[] parts = email.split("@");
        String localPart = parts[0].toLowerCase();
        String domain = parts[1].toLowerCase();
        
        // Check local part for suspicious patterns
        for (String pattern : SUSPICIOUS_PATTERNS) {
            if (localPart.equals(pattern) || localPart.contains(pattern)) {
                return true;
            }
        }
        
        // Check for random-looking emails (too many numbers or special chars)
        long digitCount = localPart.chars().filter(Character::isDigit).count();
        if (digitCount > localPart.length() * 0.5 && localPart.length() > 5) {
            return true;
        }
        
        // Check for invalid/suspicious TLD
        if (hasInvalidTld(domain)) {
            return true;
        }
        
        // Check for suspicious domain patterns
        if (hasSuspiciousDomain(domain)) {
            return true;
        }
        
        return false;
    }

    /**
     * Check if domain has an invalid or suspicious TLD.
     */
    public boolean hasInvalidTld(String domain) {
        if (domain == null || !domain.contains(".")) {
            return true; // No TLD at all is suspicious
        }
        
        String tld = domain.substring(domain.lastIndexOf(".") + 1).toLowerCase();
        
        // TLD should be at least 2 characters
        if (tld.length() < 2) {
            return true;
        }
        
        // TLD should not be more than 6 characters (most are 2-4)
        if (tld.length() > 6) {
            return true;
        }
        
        // Check against known valid TLDs
        if (!VALID_TLDS.contains(tld)) {
            // Could be a country code we don't have, but flag as suspicious
            // Only flag if it looks clearly invalid (not 2-letter country codes)
            if (tld.length() > 2) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Check if domain name looks suspicious/fake.
     */
    public boolean hasSuspiciousDomain(String domain) {
        if (domain == null) {
            return false;
        }
        
        // Get domain name without TLD
        String domainName = domain.contains(".") 
            ? domain.substring(0, domain.lastIndexOf(".")).toLowerCase()
            : domain.toLowerCase();
        
        // Very short domain names are suspicious (like "ceo", "abc")
        if (domainName.length() <= 3 && !isKnownShortDomain(domainName)) {
            return true;
        }
        
        // Check for suspicious domain patterns
        for (String pattern : SUSPICIOUS_DOMAINS_PATTERNS) {
            if (domainName.equals(pattern)) {
                return true;
            }
        }
        
        // Domain with all same characters (aaa.com, bbb.net)
        if (domainName.length() >= 3 && domainName.chars().distinct().count() == 1) {
            return true;
        }
        
        return false;
    }

    /**
     * Check if it's a known legitimate short domain.
     */
    private boolean isKnownShortDomain(String domain) {
        Set<String> knownShort = Set.of(
            "ibm", "hp", "att", "bbc", "cnn", "nbc", "abc", "fox",
            "mit", "cal", "nyu", "cia", "fbi", "dhs", "irs", "epa",
            "who", "un", "eu", "uk", "usa", "nyc", "la", "sf"
        );
        return knownShort.contains(domain.toLowerCase());
    }

    /**
     * Get risk level for an email address.
     * Returns: "HIGH" for disposable or very suspicious, "MEDIUM" for suspicious patterns, "LOW" for normal
     */
    public String getEmailRiskLevel(String email) {
        if (isDisposableEmail(email)) {
            return "HIGH";
        }
        
        // Check for multiple suspicious indicators (makes it HIGH risk)
        int suspicionScore = 0;
        
        if (email != null && email.contains("@")) {
            String[] parts = email.split("@");
            String localPart = parts[0].toLowerCase();
            String domain = parts[1].toLowerCase();
            
            // Check local part patterns
            for (String pattern : SUSPICIOUS_PATTERNS) {
                if (localPart.equals(pattern) || localPart.contains(pattern)) {
                    suspicionScore++;
                    break;
                }
            }
            
            // Check for invalid TLD
            if (hasInvalidTld(domain)) {
                suspicionScore += 2; // Invalid TLD is more severe
            }
            
            // Check for suspicious domain
            if (hasSuspiciousDomain(domain)) {
                suspicionScore++;
            }
        }
        
        // Multiple indicators = HIGH risk
        if (suspicionScore >= 2) {
            return "HIGH";
        }
        
        if (hasSuspiciousPattern(email)) {
            return "MEDIUM";
        }
        
        return "LOW";
    }

    /**
     * Check if email should be blocked from registration.
     * Blocks HIGH risk emails (disposable + multiple suspicious indicators).
     */
    public boolean shouldBlockRegistration(String email) {
        String riskLevel = getEmailRiskLevel(email);
        return "HIGH".equals(riskLevel);
    }

    /**
     * Get detailed analysis of why an email is suspicious.
     */
    public java.util.Map<String, Object> analyzeEmail(String email) {
        java.util.Map<String, Object> analysis = new java.util.HashMap<>();
        analysis.put("email", email);
        analysis.put("riskLevel", getEmailRiskLevel(email));
        analysis.put("isDisposable", isDisposableEmail(email));
        analysis.put("hasSuspiciousPattern", hasSuspiciousPattern(email));
        analysis.put("shouldBlock", shouldBlockRegistration(email));
        
        java.util.List<String> reasons = new java.util.ArrayList<>();
        
        if (email != null && email.contains("@")) {
            String[] parts = email.split("@");
            String localPart = parts[0].toLowerCase();
            String domain = parts[1].toLowerCase();
            
            // Check disposable domain
            if (isDisposableEmail(email)) {
                reasons.add("Known disposable email domain");
            }
            
            // Check local part patterns
            for (String pattern : SUSPICIOUS_PATTERNS) {
                if (localPart.equals(pattern) || localPart.contains(pattern)) {
                    reasons.add("Suspicious username pattern: contains '" + pattern + "'");
                    break;
                }
            }
            
            // Check for invalid TLD
            if (hasInvalidTld(domain)) {
                String tld = domain.substring(domain.lastIndexOf(".") + 1);
                reasons.add("Invalid or unknown TLD: '." + tld + "'");
            }
            
            // Check for suspicious domain
            if (hasSuspiciousDomain(domain)) {
                String domainName = domain.contains(".") 
                    ? domain.substring(0, domain.lastIndexOf("."))
                    : domain;
                reasons.add("Suspicious domain name: '" + domainName + "'");
            }
            
            // Check for too many numbers
            long digitCount = localPart.chars().filter(Character::isDigit).count();
            if (digitCount > localPart.length() * 0.5 && localPart.length() > 5) {
                reasons.add("Too many numbers in username (looks auto-generated)");
            }
        }
        
        analysis.put("reasons", reasons);
        return analysis;
    }
}
