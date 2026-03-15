const fs = require('fs');
const path = require('path');

const target = String.raw`C:\Claude_workspace\Hotel2025\hotel-frontend -web\target`;

console.log('Iterative deletion with Node.js...');
console.time('total');

// Strategy: Find the deepest directory iteratively, then delete bottom-up
function findDeepestPath(startPath) {
    let current = startPath;
    let depth = 0;
    while (true) {
        try {
            const entries = fs.readdirSync(current, { withFileTypes: true });
            const dirs = entries.filter(e => e.isDirectory());
            if (dirs.length === 0) break;
            current = path.join(current, dirs[0].name);
            depth++;
        } catch {
            break;
        }
    }
    return { path: current, depth };
}

// Iteratively delete from the bottom up
let totalDeleted = 0;
let pass = 0;

while (fs.existsSync(target)) {
    pass++;

    // Find the deepest point
    const deepest = findDeepestPath(target);

    if (deepest.depth === 0) {
        // Target dir itself has no subdirs, delete files and remove it
        try {
            const entries = fs.readdirSync(target, { withFileTypes: true });
            for (const e of entries) {
                const p = path.join(target, e.name);
                if (e.isFile()) fs.unlinkSync(p);
            }
            fs.rmdirSync(target);
            totalDeleted++;
        } catch (err) {
            console.log(`Final cleanup error: ${err.message}`);
        }
        break;
    }

    // Delete from deepest going up, deleting as many empty dirs as possible
    let current = deepest.path;
    let deletedThisPass = 0;

    while (current !== target && current !== path.dirname(target)) {
        try {
            // Delete all files in this dir
            const entries = fs.readdirSync(current, { withFileTypes: true });
            const hasSubdirs = entries.some(e => e.isDirectory());

            if (!hasSubdirs) {
                // Delete all files
                for (const e of entries) {
                    if (e.isFile()) {
                        fs.unlinkSync(path.join(current, e.name));
                    }
                }
                // Remove this empty directory
                fs.rmdirSync(current);
                deletedThisPass++;
                totalDeleted++;
                current = path.dirname(current);
            } else {
                break; // Has subdirs, stop going up
            }
        } catch {
            break;
        }
    }

    if (deletedThisPass === 0) {
        console.log('No progress made, stopping.');
        break;
    }

    if (pass % 100 === 0) {
        console.log(`Pass ${pass}: deleted ${totalDeleted} dirs total (depth was ${deepest.depth})`);
    }
}

console.timeEnd('total');

if (!fs.existsSync(target)) {
    console.log(`SUCCESS! Deleted ${totalDeleted} directories.`);
} else {
    console.log(`Still exists after ${totalDeleted} deletions.`);
}
