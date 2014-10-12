#include <string>
#include <sstream>
#include <systemd/sd-journal.h>
#include "log4j-systemd-journal-adapter.h"

extern "C" {

JNIEXPORT void JNICALL Java_de_bwaldvogel_log4j_SystemdJournalAdapter_sendv(JNIEnv * env, jclass obj, jint num_keys, jobjectArray keys, jobjectArray values)
{
    std::string key_values[num_keys];

    for (int i = 0; i < num_keys; i++) {
        const jstring key = (jstring)env->GetObjectArrayElement(keys, i);
        const jstring value = (jstring)env->GetObjectArrayElement(values, i);
        const char *rawKey = env->GetStringUTFChars(key, 0);
        const char *rawValue = env->GetStringUTFChars(value, 0);
        std::ostringstream o;
        o << rawKey << "=" << rawValue;
        std::string str = o.str();
        key_values[i] = str.c_str();
    }

    struct iovec v[num_keys];

    for (int i = 0; i < num_keys; i++) {
        v[i].iov_base = (void*) key_values[i].c_str();
        v[i].iov_len = key_values[i].size();
    }

    sd_journal_sendv(v, num_keys);
}

}
