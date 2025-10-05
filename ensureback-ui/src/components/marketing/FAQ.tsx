import { useState } from 'react';
import { motion } from 'framer-motion';
import { ChevronDown } from 'lucide-react';

const faqs = [
  {
    question: 'Pricing',
    answer:
      'Usage-based pricing aligned to Stripe volume. Only pay when protection is active on a payment. Contact us for an exact quote.'
  },
  {
    question: 'Data',
    answer:
      'EnsureBack listens to Stripe webhooks and reads payment metadata to run fraud heuristics. No additional instrumentation is required.'
  },
  {
    question: 'Security',
    answer:
      'SOC 2 controls, TLS 1.3, encrypted secrets, and a dedicated security team make sure buyer and merchant data stay protected.'
  },
  {
    question: 'Setup',
    answer:
      'Connect your Stripe account, import historical disputes, and enable the first automation playbook in under 20 minutes.'
  }
];

export const FAQ = () => {
  const [openIndex, setOpenIndex] = useState<number | null>(0);

  return (
    <section id="faq" className="px-4 pb-24">
      <div className="mx-auto max-w-3xl space-y-8">
        <h2 className="text-3xl font-semibold tracking-tight text-foreground sm:text-4xl">Questions teams ask</h2>
        <div className="space-y-4">
          {faqs.map((faq, index) => {
            const isOpen = openIndex === index;
            return (
              <div key={faq.question} className="rounded-2xl border border-muted bg-card">
                <button
                  type="button"
                  className="flex w-full items-center justify-between gap-4 px-6 py-4 text-left text-base font-medium text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  onClick={() => setOpenIndex(isOpen ? null : index)}
                  aria-expanded={isOpen}
                >
                  <span>{faq.question}</span>
                  <ChevronDown
                    aria-hidden="true"
                    className={`h-5 w-5 transition-transform ${isOpen ? 'rotate-180' : ''}`}
                  />
                </button>
                <motion.div
                  initial={false}
                  animate={isOpen ? 'open' : 'collapsed'}
                  variants={{
                    open: { height: 'auto', opacity: 1 },
                    collapsed: { height: 0, opacity: 0 }
                  }}
                  transition={{ duration: 0.2, ease: 'easeInOut' }}
                  className="overflow-hidden"
                >
                  <p className="px-6 pb-6 text-sm text-muted-foreground">{faq.answer}</p>
                </motion.div>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
};
